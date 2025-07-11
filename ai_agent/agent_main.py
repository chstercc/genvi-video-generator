import os
import requests
from dotenv import load_dotenv
from langchain.prompts import PromptTemplate
from langchain.chains import LLMChain
from langchain_openai import ChatOpenAI

# 加载 .env 文件
load_dotenv()

def augment_prompt(query: str, knowledgebases: list, api_key: str):
    """从知识库获取相关背景信息"""
    API_URL = "https://qianfan.baidubce.com/v2/knowledgebases/query"
    HEADERS = {
        "Content-Type": "application/json",
        "Authorization": f"Bearer {api_key}"
    }
    payload = {
        "query": query,
        "knowledgebase_ids": knowledgebases
    }

    print(f"🔍 正在调用知识库 API，query='{query}', knowledgebase_ids={knowledgebases}")

    try:
        response = requests.post(url=API_URL, headers=HEADERS, json=payload)
        print(f"知识库 API 响应状态码: {response.status_code}")
        print(f"知识库 API 响应内容: {response.text}")
        results = response.json()
    except Exception as e:
        print(f"知识库 API 请求失败: {e}")
        return ""

    chunks = results.get("chunks", [])
    print(f"知识库召回片段数: {len(chunks)}")

    if not chunks:
        return ""  # 没有召回到内容，返回空字符串

    source_knowledge = "\n".join([x["content"] for x in chunks[:3]])  # 取前3段

    augmented_prompt = f"""参考以下背景信息，生成一个简洁而完整的故事梗概（约200字）：
背景信息：
{source_knowledge}

故事标题：{query}

请务必按照以下格式返回内容（注意：必须包含标题和梗概两部分）：

标题：{query}
梗概：
[在这里开始写故事内容]
"""
    return augmented_prompt

class StoryAgent:
    def __init__(self):
        api_key = os.getenv("DEEPSEEK_API_KEY")
        if not api_key:
            raise ValueError("DEEPSEEK_API_KEY environment variable is not set")

        self.llm = ChatOpenAI(
            model="deepseek-chat",
            openai_api_key=api_key,
            openai_api_base="https://api.deepseek.com/v1",
            temperature=0.7
        )

    def _process_response(self, response_text):
        # 注意：invoke 返回的 response 直接是字符串或 Message 内容
        # 如果是 Message 对象，需要 response_text.content
        if hasattr(response_text, "content"):
            response_text = response_text.content

        lines = [line.strip() for line in response_text.split('\n') if line.strip()]
        content_lines = []
        for line in lines:
            if line.startswith('梗概：') or line.startswith('标题：'):
                continue
            content_lines.append(line)
        return '\n'.join(content_lines)

    def generate_story(self, title, knowledgebases, qianfan_api_key):
        """结合 RAG 生成故事梗概"""
        prompt = augment_prompt(title, knowledgebases, qianfan_api_key)
        if not prompt:
            raise ValueError("未能从知识库获取到相关背景信息")

        print(f"\n=== 拼接后的 Prompt ===\n{prompt}\n")
        response = self.llm.invoke(prompt)
        return self._process_response(response)

    def modify_story(self, title, summary, instruction, knowledgebases, qianfan_api_key):
        """结合 RAG 修改故事梗概"""
        prompt = augment_prompt(title, knowledgebases, qianfan_api_key)
        prompt += f"""

当前梗概：
{summary}

修改要求：
{instruction}

请按照以下格式返回修改后的内容：

标题：{title}
梗概：
[在这里写修改后的故事内容]
"""
        response = self.llm.invoke(prompt)
        return self._process_response(response)

if __name__ == "__main__":
    agent = StoryAgent()

    title = input("请输入故事标题：")
    qianfan_api_key = os.getenv("QIANFAN_API_KEY")
    knowledgebase_ids = ["KNOWLEDGEBASE_IDS"]

    try:
        result = agent.generate_story(title, knowledgebase_ids, qianfan_api_key)
        print("\n=== 📝 生成的故事梗概 ===")
        print(f"标题：{title}")
        print(f"梗概：\n{result}")

        while True:
            instruction = input("\n💬 你想修改故事吗？(输入修改要求或直接按 Enter 完成): ").strip()
            if not instruction:
                break

            result = agent.modify_story(title, result, instruction, knowledgebase_ids, qianfan_api_key)
            print("\n=== 修改后的故事梗概 ===")
            print(f"标题：{title}")
            print(f"梗概：\n{result}")
    except Exception as e:
        print(f"错误：{str(e)}")
