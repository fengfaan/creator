这是一份基于我们前述讨论为你量身定制的完整文档集。内容已经全面转向了**“本地化、个人专属、Web前端展示、Java + LangChain4j + Playwright 后端驱动”**的架构。
你可以直接将这些内容复制到你的知识库或 Markdown 编辑器中作为项目启动基石。
📄 产品需求文档 (PRD)：个人智能写作与分发工作台
1. 产品概述
   本产品是一款为个人开发者/创作者打造的本地优先 (Local-First) 智能写作与分发工具。通过大模型 API 提供多智能体写作辅助，并利用无头浏览器 (Headless Browser) 技术实现跨内容平台（微信公众号、小红书）的自动化发布。
2. 核心定位与原则
   • 本地优先，绝对掌控： 不依赖云端数据库，数据资产以 Markdown 文件形式本地存储。
   • 零服务器成本： 运行在本地机器，通过 Web 网页提供多端（PC/手机）访问体验。
   • 降维分发： 劫持本地浏览器会话，规避各大平台的反爬虫和验证码机制。
3. 核心功能模块
   模块名称	功能描述	核心价值
   Markdown 极简创作中心	支持标准 Markdown 语法，纯文本编辑。支持图片拖拽至本地文件夹并映射。	沉浸式创作，摆脱富文本排版干扰。
   Agentic AI 魔法面板	内置大纲生成 (Planner)、段落改写 (Rewriter)、小红书风格润色 (Polisher) 等 AI 动作。	缩短高质量内容的产出周期。
   多平台实时预览	PC 端支持双栏视图，右侧可实时渲染公众号 HTML 样式或小红书卡片样式。	所见即所得，避免跨端格式错乱。
   RPA 一键分发引擎	调用本地 Chrome 数据，自动完成微信公众号图文上传与小红书笔记发布。	解决手动传图、排版、复制粘贴的繁琐操作。
4. 关键业务流程
1. 内容输入： 用户在 Web 端新建 .md 文档，输入核心想法。
2. AI 协作： 框选文本，触发 LangChain4j 驱动的特定 Agent 进行扩写或风格转换。
3. 格式校验： 切换右侧 Tab，预览特定平台的排版效果，并由 AI 校验是否包含违禁词。
4. 自动化执行： 点击发布，Java 后端唤起 Playwright，控制本地 Chrome 完成表单填充与提交，页面底部输出执行日志。
   🎨 设计文档 (UI/UX Design)
   本部分聚焦于 Web 端的响应式界面设计方案，确保在 PC 的宽屏和手机的窄屏下都能获得极佳的交互体验。UI 稿建议在 Figma 中采用模块化的组件快速搭建。
1. 视觉与交互规范
   • 设计风格： 极客风 (Geek/Clean)，类似 Notion 或 Obsidian 的网页版。
   • 主题色： 终端黑 (#1E1E1E) 与 极简白 (#FAFAFA) 作为双轨主题色，辅以平台色（微信绿、小红书红）作为操作按钮高亮。
   • 响应式策略： • PC 端 (宽屏)： 采用左右双栏布局（左侧编辑，右侧预览/控制台）。 • Mobile 端 (窄屏)： 采用底部 Tab 导航，编辑区、预览区、操作区分布在不同的 Tab 标签页中。
2. PC Web 布局设计 (双栏流)
   • 全局顶栏： Logo、当前文件名称、大模型切换下拉框（如 DeepSeek / Claude）。
   • 左栏 (编辑区)： • 纯粹的 Markdown 文本区域。 • Slash Command： 输入 / 呼出悬浮菜单，提供快捷 AI 指令（如 /大纲, /扩写）。
   • 右栏 (预览与分发区)： • 顶部两段式开关：[微信公众号 预览] | [小红书 预览]。 • 中间为模拟对应平台样式的 HTML 渲染器。 • 底部固定为分发控制台：包含【一键执行发布】按钮和类似终端的黑底白字日志输出窗。
3. 手机 Web 布局设计 (抽屉流)
   • 主界面： 全屏的 Markdown 编辑器，专注于碎片化灵感的记录。
   • 底部悬浮操作区 (FAB)： 提供一个主控按钮，点击后弹出底部抽屉 (Bottom Sheet)。 • 抽屉选项 A (AI 操作)： 改写、生成大纲、提炼标签。 • 抽屉选项 B (多端预览)： 跳转到独立的预览页面。 • 抽屉选项 C (执行分发)： 触发后端发布逻辑。
   💻 研发文档 (R&D Architecture)
   本部分明确技术栈选型与核心代码逻辑实现，推荐使用 JetBrains 系列工具（如 IntelliJ IDEA, WebStorm）作为主力开发环境。
1. 技术栈选型 (Tech Stack)
   层级	技术选型	说明
   前端框架	Vue 3 + Tailwind CSS	构建轻量且响应式的 Web 页面。
   后端框架	Java 21 + Spring Boot 3	提供本地 RESTful API 和 SSE 流式接口。
   大语言模型框架	LangChain4j	利用 @AiService 接口声明式管理多智能体调用。
   自动化引擎	Playwright for Java	无头浏览器方案，负责挂载本地 Chrome 缓存执行 DOM 操作。
   本地数据引擎	SQLite	仅存储配置（API Key、多端账号信息、执行日志）。核心文章使用 java.nio.file 读写 .md 文件。
2. 核心后端设计与接口 (Java / Spring Boot)
   2.1 AI Agent 模块设计
   依托 LangChain4j 的声明式服务，定义清晰的业务角色。
   // Agent 角色定义示例
   @AiService
   public interface WriterAgent {
   // 小红书风格重写
   @SystemMessage("你是一位小红书运营专家，请将用户的文本转换为包含丰富Emoji、分段短促的小红书网感风格。")
   String toXhsStyle(@UserMessage String content);

   // 大纲生成
   @SystemMessage("根据用户提供的主题，输出一份逻辑清晰的三级Markdown大纲。")
   String generateOutline(@UserMessage String topic);
   }

2.2 RPA 分发执行模块
这是绕过平台验证的核心逻辑，通过读取操作系统中真实浏览器的 User Data 实现。
// Playwright 接管本地浏览器逻辑示例
@Service
public class PublishEngine {
public void publishToXhs(String markdownContent, List<String> imagePaths) {
Playwright playwright = Playwright.create();
// 挂载 Mac 本地 Chrome 缓存路径
Path userDataDir = Paths.get(System.getProperty("user.home"), "Library/Application Support/Google/Chrome");

        BrowserType.LaunchPersistentContextOptions options = new BrowserType.LaunchPersistentContextOptions()
            .setHeadless(false) // 开启 UI 以便监控执行状态
            .setChannel("chrome");
            
        try (BrowserContext context = playwright.chromium().launchPersistentContext(userDataDir, options)) {
            Page page = context.newPage();
            // 访问小红书创作者中心
            page.navigate("https://creator.xiaohongshu.com/creator/post");
            
            // 下方编写基于 CSS Selector 的 DOM 自动化操作：
            // 1. 上传图片 (page.setInputFiles)
            // 2. 填写正文 (page.fill)
            // 3. 点击发布 (page.click)
        }
    }
}

2.3 API 路由设计
• POST /api/v1/ai/generate：接收前端指令，返回大模型结果（支持 SSE 流式返回打字机效果）。
• POST /api/v1/files/save：将前端编辑的内容持久化写入本地 .md 文件。
• POST /api/v1/publish/execute：前端发起发布指令，触发 PublishEngine。
• WS /ws/logs：建立 WebSocket 连接，将 Playwright 执行的每一步日志（如 [INFO] 正在点击上传图片）实时推送至前端展示。
3. 本地运行与部署
   作为个人工具，环境的打包和启动需要尽可能简单。
1. 数据目录规划： 系统在首次启动时，自动在用户文档目录下生成 .ai-publisher/ 目录，包含 sqlite.db 配置文件和 assets/ 图片资源文件夹。
2. 前后端整合： 在开发完成后，将 Vue 3 编译产物 (dist 目录) 移入 Spring Boot 的 src/main/resources/static 目录中。
3. 单文件运行： 最终通过 Maven/Gradle 打包成一个独立的 ai-publisher.jar。日常只需一条 java -jar 命令即可启动服务，随后在浏览器中访问 http://localhost:8080 即可开始创作。