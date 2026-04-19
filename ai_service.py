from flask import Flask, request, jsonify
from config import AI_API_KEY, AI_BASE_URL, AI_MODEL_NAME
from openai import OpenAI
import json
import re
app = Flask(__name__)

client = OpenAI(
    api_key=AI_API_KEY,
    base_url=AI_BASE_URL,
)
MODEL_NAME = AI_MODEL_NAME


SYSTEM_PROMPT = """你是一个任务信息提取专家。用户会用自然语言描述一个待办任务，你需要从中提取结构化的任务信息。

## 提取规则

### title（任务标题）
- 简洁概括任务核心内容，不超过20个字
- 去掉时间、情绪等修饰词，只保留动作+对象
- 示例："买菜" "与老板开会" "完成项目报告"

### description（任务描述）
- 补充标题未能表达的细节，如时间、地点、具体要求
- 如果用户描述本身已很简短且无额外细节，可留空字符串 ""
- 不要重复标题内容

### priority（优先级）
判断依据（按以下信号综合判断）：
- **high（高）**：含"紧急""马上""立刻""非常重要""critical""urgent"，或截止时间在24小时内
- **medium（中）**：含"尽快""比较重要""近期"，或截止时间在一周内
- **low（低）**：没有紧迫信号，或含"有空""随时""不急"

### status（状态）
- 新创建的任务一律返回 "pending"

### tags（标签数组）
- 从内容中提取1到4个关键词作为标签
- 优先识别：场景类（工作/学习/生活/健康/财务）、人物类（老板/客户/团队）、类型类（会议/报告/购物/运动）
- 全部小写，不加"#"符号
- 不要把时间词（今天/明天/下午）作为标签

## 输出格式
只输出一个合法的 JSON 对象，禁止有任何多余文字、解释或 Markdown 代码块。格式如下：
{
  "title": "...",
  "description": "...",
  "priority": "low" | "medium" | "high",
  "status": "pending",
  "tags": ["tag1", "tag2"]
}"""


def extract_json(text: str) -> dict:
    """从模型回复中健壮地提取 JSON 对象"""
    # 尝试直接解析
    try:
        return json.loads(text.strip())
    except json.JSONDecodeError:
        pass

    # 尝试提取 ```json ... ``` 代码块
    match = re.search(r"```(?:json)?\s*(\{.*?\})\s*```", text, re.DOTALL)
    if match:
        try:
            return json.loads(match.group(1))
        except json.JSONDecodeError:
            pass

    # 尝试提取第一个 { ... } 块
    match = re.search(r"\{.*\}", text, re.DOTALL)
    if match:
        try:
            return json.loads(match.group(0))
        except json.JSONDecodeError:
            pass

    raise ValueError(f"无法从模型回复中解析 JSON，原始内容：{text}")


@app.route("/generate", methods=["POST"])
def generate_task():
    """
    接收 Spring Boot 转发的请求，调用 LLM 提取任务信息
    请求体: { "prompt": "用户输入的自然语言" }
    返回:   { "title": "...", "description": "...", "priority": "...", "status": "pending", "tags": [...] }
    """
    data = request.get_json()
    if not data or not data.get("prompt"):
        return jsonify({"error": "缺少 prompt 参数"}), 400

    user_prompt = data["prompt"].strip()
    if len(user_prompt) > 500:
        return jsonify({"error": "输入内容过长，请控制在500字以内"}), 400

    try:
        response = client.chat.completions.create(
            model=MODEL_NAME,
            messages=[
                {"role": "system", "content": SYSTEM_PROMPT},
                {"role": "user",   "content": user_prompt},
            ],
            temperature=0.3,   # 低温度保证输出稳定
            max_tokens=300,
        )

        raw_text = response.choices[0].message.content
        task_data = extract_json(raw_text)

        # 字段校验与兜底
        task_data.setdefault("title",       user_prompt[:20])
        task_data.setdefault("description", "")
        task_data.setdefault("status",      "pending")
        task_data.setdefault("tags",        [])

        if task_data.get("priority") not in ("low", "medium", "high"):
            task_data["priority"] = "medium"

        # 强制 status 为 pending（新创建任务）
        task_data["status"] = "pending"

        return jsonify(task_data), 200

    except ValueError as e:
        return jsonify({"error": f"JSON解析失败: {str(e)}"}), 500
    except Exception as e:
        return jsonify({"error": f"AI服务内部错误: {str(e)}"}), 500


@app.route("/health", methods=["GET"])
def health():
    """健康检查，Spring Boot 启动时可调用确认 Python 服务存活"""
    return jsonify({"status": "ok", "model": MODEL_NAME}), 200


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5001, debug=False)