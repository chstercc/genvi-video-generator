from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
from typing import Optional
from agent_main import StoryAgent
import logging
import os
from dotenv import load_dotenv

# 加载 .env 文件
load_dotenv()

# 配置日志
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI()

# 添加CORS中间件
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:5173"],  # 允许的前端源
    allow_credentials=True,
    allow_methods=["*"],  # 允许所有HTTP方法
    allow_headers=["*"],  # 允许所有headers
)

# 初始化 StoryAgent
agent = StoryAgent()

# 从环境变量中获取 QIANFAN_API_KEY 和 KNOWLEDGEBASE_IDS
qianfan_api_key = os.getenv("QIANFAN_API_KEY")
knowledgebase_ids_str = os.getenv("KNOWLEDGEBASE_IDS")

if not qianfan_api_key or not knowledgebase_ids_str:
    raise ValueError("Missing QIANFAN_API_KEY or KNOWLEDGEBASE_IDS in .env")

knowledgebase_ids = knowledgebase_ids_str.split(',')

class StoryRequest(BaseModel):
    title: str = Field(..., min_length=1, max_length=100, description="故事标题")

class ModifyRequest(BaseModel):
    title: str = Field(..., min_length=1, max_length=100, description="故事标题")
    summary: str = Field(..., description="当前故事梗概")
    instruction: str = Field(..., description="修改指令")

@app.post("/generate-story")
async def generate_story(request: StoryRequest):
    try:
        logger.info(f"Received story generation request with title: {request.title}")
        
        # 生成故事
        summary = agent.generate_story(request.title, knowledgebase_ids, qianfan_api_key)
        
        logger.info("Story generated successfully")
        return {
            "success": True,
            "summary": summary,
            "message": "Story generated successfully"
        }
    except Exception as e:
        logger.error(f"Error generating story: {str(e)}")
        raise HTTPException(
            status_code=500,
            detail=f"Error generating story: {str(e)}"
        )

@app.get("/health")
async def health_check():
    return {"status": "healthy"}

@app.post("/modify-story")
async def modify_story(request: ModifyRequest):
    try:
        logger.info(f"Received story modification request")
        
        # 修改故事
        modified_summary = agent.modify_story(
            request.title,
            request.summary,
            request.instruction,
            knowledgebase_ids,
            qianfan_api_key
        )
        
        logger.info("Story modified successfully")
        return {
            "success": True,
            "summary": modified_summary,
            "message": "Story modified successfully"
        }
    except Exception as e:
        logger.error(f"Error modifying story: {str(e)}")
        raise HTTPException(
            status_code=500,
            detail=f"Error modifying story: {str(e)}"
        )