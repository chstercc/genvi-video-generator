from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
from typing import Optional
from agent_main import StoryAgent
import logging

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

class StoryRequest(BaseModel):
    title: str = Field(..., min_length=1, max_length=100, description="故事标题")

@app.post("/generate-story")
async def generate_story(request: StoryRequest):
    try:
        logger.info(f"Received story generation request with title: {request.title}")
        
        # 生成故事
        summary = agent.generate_story(request.title)
        
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
