## 项目标题 (Project Title)

智能任务管理系统 (Intelligent Task Management System)



## 角色定位(Role Track)

AI/LLM 开发轨道 (AI/LLM Developer Track)



## 技术栈(Tech Stack)

本系统采用 \*\*Java Spring Boot 后端 + Vue 3 前端 + Python Flask AI 服务\*\*的架构。通过独立的 AI 微服务层，实现了基于 LLM 的任务智能化处理，包括自然语言解析、逻辑拆解与语义检索。



## 已实现功能列表 (Features Implemented)

CRUD、基本的前后端架构、AI智能生成任务、AI智能分解任务、AI智能检索任务



## 安装步骤 (Setup Instructions)

1、配置python解释器，根据requirement安装依赖。<br>

2、导入数据库，sql文件在根目录<br>

3、先启动ai\_service.py，再启动src/main/java/task_manager/entity/Task.java<br>


## API 文档 (API Documentation)
config.py: <br>
AI_API_KEY = "sk-8cc907e63faf49818c1a27272dc3fbd8"<br>
AI_BASE_URL = "https://api.deepseek.com"<br>
AI_MODEL_NAME = "deepseek-chat"<br>

## 设计决策 (Design Decisions)
本系统的前后端代码由AI生成，AI智能功能设计由个人生成，AI辅助设计Python和Java后端之间的连接。<br>
本来一开始想使用若依系统作为前后端的框架，但是若依系统对于此任务过于庞大，因此使用了最简单的单界面前端，Spring Boot的后端，Mysql数据库。Spring Boot提供了CRUD的接口，因此基础的CRUD功能实现很简单（反正我是写不出来比Vue更好的前端架构和比SpringBoot更好的后端架构）。<br>
时间有限，AI部分的设计使用了第三方的api，来自于DeepSeek。但其实最保险的方案应该是结合了本地模型与传统文本处理手段结合，速度响应和成本都最低（但是要衡量本地部署模型的参数量）。机器学习算法的方案因为本次任务缺少数据集训练而被否决。<br>

## 挑战与解决方案 (Challenges & Solutions)
AI功能的设计高度依赖于Prompt的设计，我测试了很多次，最后还是决定选择严格输出JSON文本的方式。<br>

## 未来改进方向 (Future Improvements)
功能3，AI智能搜索的功能测试效果不是很好，这牵涉到了一个权限的问题，我暂时没想到一个比较好的方案可以让AI检索数据库内部的任务描述字段，这样检索的过程能更智能，另外语意的匹配也无需百分比精确，可以设计一个基于模型词表的概率分布匹配网络。<br>

## 投入时间 (Time Spent)
实际coding时间: 4小时<br>
若干方案思考时间<br>