from dotenv import load_dotenv
import os

load_dotenv()
print(os.getenv("DEEPSEEK_API_KEY"))
print(os.getenv("QIANFAN_API_KEY"))
print(os.getenv("KNOWLEDGEBASE_IDS"))