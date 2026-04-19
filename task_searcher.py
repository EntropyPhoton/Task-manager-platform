from flask import Blueprint, request, jsonify
from openai import OpenAI
import re
import config

searcher_bp = Blueprint("searcher", __name__)

client = OpenAI(
    api_key=config.AI_API_KEY,
    base_url=config.AI_BASE_URL,
)
MODEL_NAME = config.AI_MODEL_NAME

SEARCH_SYSTEM_PROMPT = """你是一个搜索关键词提炼专家。用户会用自然语言描述他想查找的任务，你需要从中提炼出最适合用于数据库标题搜索的关键词。

## 提炼规则

1. **只返回一个关键词**：选择最能代表用户搜索意图的核心词，不要返回短语或多个词
2. **优先选择名词**：动词、形容词通常不是好的搜索词，优先选事物、人物、场景名词
3. **去掉干扰词**：忽略"帮我找""查一下""有没有""相关的"等表达搜索意图的词，这些不是关键词
4. **保持原始形态**：不要翻译、缩写或变形，直接用用户描述中出现的词或其核心部分

## 示例

| 用户输入 | 你的输出 |
|---------|---------|
| 找一下跟老板开会相关的任务 | 会议 |
| 有没有关于学习 Python 的待办事项 | Python |
| 查找所有标记为工作的任务 | 工作 |
| 我想找买东西相关的 | 购物 |
| 帮我搜一下健身计划 | 健身 |

## 输出格式
只输出关键词本身，不加任何标点、引号或解释。例如：
会议"""


def extract_keyword(text: str) -> str:
    """清理模型返回的关键词，去除多余字符"""
    # 去除引号、标点、空白
    text = text.strip().strip('"\'""''。，、')
    # 如果模型返回了多个词（用空格或逗号分隔），取第一个
    text = re.split(r'[\s,，、/]', text)[0]
    return text.strip()


@searcher_bp.route("/smart-search", methods=["POST"])
def smart_search():
    """
    从自然语言描述中提炼搜索关键词。

    请求体: { "prompt": "用户的自然语言搜索描述" }
    返回:   { "keyword": "提炼出的关键词" }
    """
    data = request.get_json()
    if not data or not data.get("prompt"):
        return jsonify({"error": "缺少 prompt 参数"}), 400

    user_prompt = data["prompt"].strip()
    if len(user_prompt) > 200:
        return jsonify({"error": "搜索描述过长，请控制在200字以内"}), 400

    try:
        response = client.chat.completions.create(
            model=MODEL_NAME,
            messages=[
                {"role": "system", "content": SEARCH_SYSTEM_PROMPT},
                {"role": "user",   "content": user_prompt},
            ],
            temperature=0.1,
            max_tokens=20,
        )

        raw = response.choices[0].message.content
        keyword = extract_keyword(raw)

        if not keyword:
            # 兜底：直接取用户输入的前10个字
            keyword = user_prompt[:10]

        return jsonify({"keyword": keyword}), 200

    except Exception as e:
        return jsonify({"error": f"AI 服务内部错误: {str(e)}"}), 500