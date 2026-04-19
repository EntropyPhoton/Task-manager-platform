from flask import Blueprint, request, jsonify
from config import *
from openai import OpenAI
import json
import re

decomposer_bp = Blueprint("decomposer", __name__)

client = OpenAI(
    api_key=AI_API_KEY,
    base_url=AI_BASE_URL,
)
MODEL_NAME = AI_MODEL_NAME


DECOMPOSE_SYSTEM_PROMPT = """你是一个项目规划专家。你的唯一职责是：将用户描述的复杂任务强制拆解为多个可独立执行的子任务。

## 核心规则（必须严格遵守）

1. **永远输出数组**：无论用户输入多简短，你都必须输出包含至少3个元素的 JSON 数组，绝对不能输出单个对象
2. **主动识别隐含步骤**：用户不会把每个步骤都说出来，你需要凭借专业知识补全完成该目标所必需的所有步骤
3. **按执行顺序排列**：数组第一个元素是最先要做的事，最后一个是最后做的事
4. **子任务数量**：3～9 个，粒度适中

## 子任务字段说明

### title
- 动词开头，简洁描述该步骤的核心动作，不超过15字
- 例："调研竞品方案" / "搭建开发环境" / "编写单元测试"

### description
- 说明该步骤的具体内容或完成标准，1-2句话
- 内容简单时可留空字符串 ""

### tags
- 1～3个关键词，反映子任务类型或所属阶段，全部小写
- 常见值：调研、设计、开发、测试、部署、文档、沟通、采购、运营

## 输出格式（唯一合法格式）
只输出一个 JSON 数组，禁止任何多余文字、解释或 Markdown 代码块：
[
  {"title": "...", "description": "...", "tags": ["..."]},
  {"title": "...", "description": "...", "tags": ["..."]}
]

## 示例

用户输入：
"开发一个公司官网"

你的输出：
[
  {"title": "确认需求与页面结构", "description": "与相关方确认网站目标、页面数量、核心功能和设计风格。", "tags": ["调研", "沟通"]},
  {"title": "设计UI原型", "description": "使用设计工具完成首页及各子页面的视觉稿，并通过评审。", "tags": ["设计"]},
  {"title": "搭建前端项目框架", "description": "初始化代码仓库，配置构建工具、路由和基础样式体系。", "tags": ["开发"]},
  {"title": "开发各页面功能", "description": "按照设计稿逐页实现 HTML/CSS/JS，确保响应式适配。", "tags": ["开发"]},
  {"title": "对接后端接口与数据", "description": "集成联系表单提交、内容管理等所需的后端服务。", "tags": ["开发", "对接"]},
  {"title": "测试与修复缺陷", "description": "进行功能测试、兼容性测试，修复发现的问题。", "tags": ["测试"]},
  {"title": "部署上线并配置域名", "description": "将项目部署到生产服务器，完成域名解析和 HTTPS 配置。", "tags": ["部署"]}
]"""


def extract_json_array(text: str) -> list:
    """从模型回复中健壮地提取 JSON 数组"""
    try:
        result = json.loads(text.strip())
        if isinstance(result, list):
            return result
    except json.JSONDecodeError:
        pass


    match = re.search(r"```(?:json)?\s*(\[.*?\])\s*```", text, re.DOTALL)
    if match:
        try:
            result = json.loads(match.group(1))
            if isinstance(result, list):
                return result
        except json.JSONDecodeError:
            pass


    match = re.search(r"\[.*\]", text, re.DOTALL)
    if match:
        try:
            result = json.loads(match.group(0))
            if isinstance(result, list):
                return result
        except json.JSONDecodeError:
            pass

    raise ValueError(f"无法从模型回复中解析 JSON 数组，原始内容：{text}")


def assign_priority(index: int, total: int) -> str:
    """
    根据子任务在列表中的位置分配优先级。
    执行顺序越靠前优先级越高：
      前 1/3 → high（需要先完成）
      中 1/3 → medium
      后 1/3 → low（收尾工作）
    """
    third = total / 3
    if index < third:
        return "high"
    elif index < 2 * third:
        return "medium"
    else:
        return "low"


@decomposer_bp.route("/decompose", methods=["POST"])
def decompose_task():
    """
    接收复杂任务描述，用 AI 分解为有序子任务列表。

    请求体: { "prompt": "复杂任务的自然语言描述" }
    返回:   [
              { "title": "...", "description": "...", "priority": "high",   "status": "pending", "tags": [...] },
              { "title": "...", "description": "...", "priority": "medium", "status": "pending", "tags": [...] },
              ...
            ]
    """
    data = request.get_json()
    if not data or not data.get("prompt"):
        return jsonify({"error": "缺少 prompt 参数"}), 400

    user_prompt = data["prompt"].strip()
    if len(user_prompt) > 1000:
        return jsonify({"error": "输入内容过长，请控制在1000字以内"}), 400

    try:
        response = client.chat.completions.create(
            model=MODEL_NAME,
            messages=[
                {"role": "system", "content": DECOMPOSE_SYSTEM_PROMPT},
                {"role": "user",   "content": f"请分解以下复杂任务：\n\n{user_prompt}"},
            ],
            temperature=0.4,
            max_tokens=1500,
        )

        raw_text = response.choices[0].message.content
        subtasks_raw = extract_json_array(raw_text)

        if not subtasks_raw:
            return jsonify({"error": "AI 未能识别出任何子任务，请尝试更详细的描述"}), 500

        # 限制子任务数量上限，防止 AI 过度拆解
        subtasks_raw = subtasks_raw[:9]
        total = len(subtasks_raw)

        # 组装完整任务对象（补充 priority 和 status）
        result = []
        for idx, raw in enumerate(subtasks_raw):
            task = {
                "title":       raw.get("title", f"子任务 {idx + 1}"),
                "description": raw.get("description", ""),
                "priority":    assign_priority(idx, total),
                "status":      "pending",
                "tags":        raw.get("tags", []) if isinstance(raw.get("tags"), list) else [],
            }
            result.append(task)

        return jsonify(result), 200

    except ValueError as e:
        return jsonify({"error": f"JSON 解析失败: {str(e)}"}), 500
    except Exception as e:
        return jsonify({"error": f"AI 服务内部错误: {str(e)}"}), 500