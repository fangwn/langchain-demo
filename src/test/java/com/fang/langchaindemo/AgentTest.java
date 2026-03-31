package com.fang.langchaindemo;

import com.fang.langchaindemo.domain.Cv;
import com.fang.langchaindemo.domain.CvReview;
import com.fang.langchaindemo.service.agent.*;
import com.fang.langchaindemo.tools.OrganizingTools;
import com.fang.langchaindemo.tools.RagProvider;
import com.fang.langchaindemo.tools.SampleTools;
import com.knuddels.jtokkit.api.ModelType;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.supervisor.SupervisorAgent;
import dev.langchain4j.agentic.supervisor.SupervisorContextStrategy;
import dev.langchain4j.agentic.supervisor.SupervisorResponseStrategy;
import dev.langchain4j.agentic.workflow.HumanInTheLoop;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiChatModel;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.FileSystemResourceLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *
 * @author fangwennan
 * @date 2026/3/25 10:11
 */
@SpringBootTest
public class AgentTest {

    @Resource
    @Qualifier("tongYiChatModel")
    private OpenAiChatModel tongYiChatModel;

    @Resource
    private RagProvider ragProvider;

    @Resource
    private SampleTools sampleTools;

    @Test
    public void testAgent() {
        CreativeWriter creativeWriter = AgenticServices.agentBuilder(CreativeWriter.class)
                .chatModel(tongYiChatModel)
                .outputKey("story")
                .build();
        String story = creativeWriter.generateStory("雷蕾");
        System.out.println("story:" + story);
    }

    @Test
    public void testAgent2() {
        CreativeWriter2 creativeWriter = AgenticServices.agentBuilder(CreativeWriter2.class)
                .chatModel(tongYiChatModel)
                .build();
        String story = creativeWriter.generateStory2("赖玫丽");
        System.out.println("story:" + story);
    }

    @Test
    public void testAgent3() throws IOException {
        CvGenerator cvGenerator = AgenticServices.agentBuilder(CvGenerator.class)
                .chatModel(tongYiChatModel)
                .outputKey("masterCv")
                .build();
        String userLifeStory = new FileSystemResourceLoader().getResource("classpath:data/user_life_story.txt").getContentAsString(StandardCharsets.UTF_8);
        String cv = cvGenerator.generateCv(userLifeStory);
        System.out.println("cv:" + cv);
    }

    @Test
    public void testAgent4() throws IOException {
        CvGeneratorStructuredOutput cvGenerator = AgenticServices.agentBuilder(CvGeneratorStructuredOutput.class)
                .chatModel(tongYiChatModel)
                .outputKey("masterCv")
                .build();
        String userLifeStory = new FileSystemResourceLoader().getResource("classpath:data/user_life_story.txt").getContentAsString(StandardCharsets.UTF_8);
        Cv cv = cvGenerator.generateCv(userLifeStory);
        System.out.println("cv:" + cv);
    }

    /**
     * 顺序工作流
     *
     * @throws IOException
     */
    @Test
    public void testAgent5() throws IOException {
        CvGenerator cvGenerator = AgenticServices.agentBuilder(CvGenerator.class)
                .chatModel(tongYiChatModel)
                .outputKey("masterCv")
                .build();
        CvTailor cvTailor = AgenticServices.agentBuilder(CvTailor.class)
                .chatModel(tongYiChatModel)
                .outputKey("tailoredCv")
                .build();
        UntypedAgent tailoredCvGenerator = AgenticServices.sequenceBuilder()
                .subAgents(cvGenerator, cvTailor)
                .outputKey("tailoredCv")
                .build();
        String liftStory = new FileSystemResourceLoader().getResource("classpath:data/user_life_story.txt").getContentAsString(StandardCharsets.UTF_8);
        String instructions = new FileSystemResourceLoader().getResource("classpath:data/job_description_backend.txt").getContentAsString(StandardCharsets.UTF_8);
        // 由于使用的是无类型Agent，需要传递参数映射
        Map<String, Object> arguments = Map.of(
                "lifeStory", liftStory,
                "instructions", instructions
        );
        // 调用合成 Agent 生成优化后的简历
        Object tailoredCv = tailoredCvGenerator.invoke(arguments);
        System.out.println("=== 优化后的简历（无类型代理）===");
        System.out.println(tailoredCv);
    }

    @Test
    public void testAgent6() throws IOException {
        CvReviewer cvReviewer = AgenticServices.agentBuilder(CvReviewer.class)
                .chatModel(tongYiChatModel)
                // 每次迭代中都会用新的反馈更新，用于下一次简历优化
                .outputKey("cvReview")
                .build();

        ScoredCvTailor scoredCvTailor = AgenticServices.agentBuilder(ScoredCvTailor.class)
                .chatModel(tongYiChatModel)
                // 每次迭代中都会更新，持续改进简历
                .outputKey("cv")
                .build();

        // 除非定义了组合Agent，否则使用UntypedAgent
        UntypedAgent reviewedCvGenerator = AgenticServices
                // 可以添加任意数量的Agent，顺序很重要
                .loopBuilder().subAgents(cvReviewer, scoredCvTailor)
                // 想要观察的最终输出（改进后的简历）
                .outputKey("cv")
                .exitCondition(agenticScope -> {
                    CvReview review = (CvReview) agenticScope.readState("cvReview");
                    // 记录中间分数
                    System.out.println("检查退出条件，当前分数=" + review.score);
                    return review.score > 0.8;
                })
                // 基于CvReviewer Agent给出的分数的退出条件，当>0.8时表示结果满意
                // 注意：退出条件在每次Agent调用后检查，而不仅仅在整个循环之后
                // 安全机制以避免无限循环，以防退出条件永远无法满足
                .maxIterations(3)
                .build();

        // 从resources/documents/中的文本文件加载原始参数：
        // - master_cv.txt
        // - job_description_backend.txt
        String masterCv = new FileSystemResourceLoader().getResource("classpath:data/master_cv.txt").getContentAsString(StandardCharsets.UTF_8);
        String jobDescription = new FileSystemResourceLoader().getResource("classpath:data/job_description_backend.txt").getContentAsString(StandardCharsets.UTF_8);

        // 因为我们使用无类型智能体，需要传递参数映射
        Map<String, Object> arguments = Map.of(
                // 从原始简历开始，持续改进
                "cv", masterCv,
                "jobDescription", jobDescription
        );

        // 调用组合 Agent 生成定制的简历
        String tailoredCv = (String) reviewedCvGenerator.invoke(arguments);

        // 打印生成的简历
        System.out.println("=== 已Review的简历（无类型）===");
        System.out.println(tailoredCv);

        // 这份简历可能在第一次定制+Review轮次后就通过了
        // 如果想看到失败的情况，可以尝试使用长笛教师的职位描述，例如：
        // String fluteJobDescription = "我们正在寻找一位充满热情的长笛教师加入我们的音乐学院。";
        // 如示例所示，也会检查简历的中间状态
        // 并检索最终的审阅意见和分数。
    }

    @Test
    public void testAgent7() throws IOException {
        // 2. 在本包中定义三个子智能体：
        //      - HrCvReviewer.java
        //      - ManagerCvReviewer.java
        //      - TeamMemberCvReviewer.java

        // 3. 使用AgenticServices创建所有智能体
        HrCvReviewer hrCvReviewer = AgenticServices.agentBuilder(HrCvReviewer.class)
                .chatModel(tongYiChatModel)
                .outputKey("hrReview") // 这将在每次迭代中被覆盖，同时也作为我们想要观察的最终输出
                .build();

        ManagerCvReviewer managerCvReviewer = AgenticServices.agentBuilder(ManagerCvReviewer.class)
                .chatModel(tongYiChatModel)
                .outputKey("managerReview") // 这会覆盖原始输入指令，并在每次迭代中被覆盖，用作CvTailor的新指令
                .build();

        TeamMemberCvReviewer teamMemberCvReviewer = AgenticServices.agentBuilder(TeamMemberCvReviewer.class)
                .chatModel(tongYiChatModel)
                .outputKey("teamMemberReview") // 这会覆盖原始输入指令，并在每次迭代中被覆盖，用作CvTailor的新指令
                .build();

        // 4. 构建执行流程
        var executor = Executors.newFixedThreadPool(3);  // 保留引用以便后续关闭

        UntypedAgent cvReviewGenerator = AgenticServices // 使用UntypedAgent，除非你定义了结果组合智能体，参见_2_Sequential_Agent_Example
                .parallelBuilder()
                .subAgents(hrCvReviewer, managerCvReviewer, teamMemberCvReviewer) // 可以添加任意多个
                .executor(executor) // 可选，默认使用内部缓存的线程池，执行完成后会自动关闭
                .outputKey("fullCvReview") // 这是我们想要观察的最终输出
                .output(agenticScope -> {
                    // 从智能体作用域读取每个评审者的输出
                    CvReview hrReview = (CvReview) agenticScope.readState("hrReview");
                    CvReview managerReview = (CvReview) agenticScope.readState("managerReview");
                    CvReview teamMemberReview = (CvReview) agenticScope.readState("teamMemberReview");
                    // 返回汇总的评审结果，包含平均分（或你想要的任何其他聚合方式）
                    String feedback = String.join("\n",
                            "HR评审: " + hrReview.feedback,
                            "经理评审: " + managerReview.feedback,
                            "团队成员评审: " + teamMemberReview.feedback
                    );
                    double avgScore = (hrReview.score + managerReview.score + teamMemberReview.score) / 3.0;

                    return new CvReview(avgScore, feedback);
                })
                .build();

        // 5. 从resources/documents/目录下的文本文件加载原始参数
        String candidateCv = new FileSystemResourceLoader().getResource("classpath:data/tailored_cv.txt").getContentAsString(StandardCharsets.UTF_8);
        String jobDescription = new FileSystemResourceLoader().getResource("classpath:data/job_description_backend.txt").getContentAsString(StandardCharsets.UTF_8);
        String hrRequirements = new FileSystemResourceLoader().getResource("classpath:data/hr_requirements.txt").getContentAsString(StandardCharsets.UTF_8);
        String phoneInterviewNotes = new FileSystemResourceLoader().getResource("classpath:data/phone_interview_notes.txt").getContentAsString(StandardCharsets.UTF_8);

        // 6. 由于我们使用了无类型智能体，需要传递参数映射
        Map<String, Object> arguments = Map.of(
                "candidateCv", candidateCv,
                "jobDescription", jobDescription
                , "hrRequirements", hrRequirements
                , "phoneInterviewNotes", phoneInterviewNotes
        );

        // 7. 调用组合智能体生成定制的简历
        var review = cvReviewGenerator.invoke(arguments);

        // 8. 打印生成的简历
        System.out.println("=== 已评审的简历 ===");
        System.out.println(review);

        // 9. 关闭执行器
        executor.shutdown();
    }

    /**
     * 条件工作流
     *
     * @throws IOException
     */
    @Test
    public void testAgent8() throws IOException {
        // 创建所有智能体
        EmailAssistant emailAssistant = AgenticServices.agentBuilder(EmailAssistant.class)
                .chatModel(tongYiChatModel)
                .tools(new OrganizingTools()) // 该智能体可以使用那里定义的所有工具
                .build();
        InterviewOrganizer interviewOrganizer = AgenticServices.agentBuilder(InterviewOrganizer.class)
                .chatModel(tongYiChatModel)
                .tools(new OrganizingTools())
                .contentRetriever(ragProvider.loadHouseRulesRetriever()) // 这是如何为智能体添加RAG的方式
                .build();
        // 构建条件式工作流
        UntypedAgent candidateResponder = AgenticServices.conditionalBuilder()
                .subAgents(agenticScope -> ((CvReview) agenticScope.readState("cvReview")).score >= 0.8, interviewOrganizer)
                .subAgents(agenticScope -> ((CvReview) agenticScope.readState("cvReview")).score < 0.8, emailAssistant)
                .build();
        String candidateCv = new FileSystemResourceLoader().getResource("classpath:data/tailored_cv.txt").getContentAsString(StandardCharsets.UTF_8);
        String candidateContact = new FileSystemResourceLoader().getResource("classpath:data/candidate_contact.txt").getContentAsString(StandardCharsets.UTF_8);
        String jobDescription = new FileSystemResourceLoader().getResource("classpath:data/job_description_backend.txt").getContentAsString(StandardCharsets.UTF_8);
        CvReview cvReviewFail = new CvReview(0.6, "简历不错，但缺少一些与后端职位相关的技术细节。");
        CvReview cvReviewPass = new CvReview(0.9, "简历非常出色，符合后端职位的所有要求。");
        // 因为我们使用了无类型智能体，所以需要传递所有输入参数的映射
        Map<String, Object> arguments = Map.of(
                "candidateCv", candidateCv,
                "candidateContact", candidateContact,
                "jobDescription", jobDescription,
                "cvReview", cvReviewFail // 更改为cvReviewFail以查看另一个分支
        );
        Object invoke = candidateResponder.invoke(arguments);
        System.out.println(invoke);
    }

    @Test
    public void testAgent9() throws IOException {
        CvGenerator cvGenerator = AgenticServices.agentBuilder(CvGenerator.class)
                .chatModel(tongYiChatModel)
                .outputKey("cv")
                .build();
        ScoredCvTailor scoredCvTailor = AgenticServices.agentBuilder(ScoredCvTailor.class)
                .chatModel(tongYiChatModel)
                .outputKey("dv")
                .build();
        CvReviewer cvReviewer = AgenticServices.agentBuilder(CvReviewer.class)
                .chatModel(tongYiChatModel)
                .outputKey("cvReview")
                .build();
        UntypedAgent cvImprovementLoop = AgenticServices.loopBuilder()
                .subAgents(scoredCvTailor, cvReviewer)
                .outputKey("cv")
                .exitCondition(agenticScope -> {
                    CvReview review = (CvReview) agenticScope.readState("cvReview");
                    System.out.println("简历评审分数: " + review.score);
                    if (review.score >= 0.8) {
                        System.out.println("简历已足够好,退出循环");
                    }
                    return review.score >= 0.8;
                })
                .maxIterations(3)
                .build();
        CandidateWorkflow candidateWorkflow = AgenticServices.sequenceBuilder(CandidateWorkflow.class)
                .subAgents(cvGenerator, cvReviewer, cvImprovementLoop)
                .outputKey("cv")
                .build();
        String lifeStory = new FileSystemResourceLoader().getResource("classpath:data/user_life_story.txt").getContentAsString(StandardCharsets.UTF_8);
        String jobDescription = new FileSystemResourceLoader().getResource("classpath:data/job_description_backend.txt").getContentAsString(StandardCharsets.UTF_8);
        String candidateCv = candidateWorkflow.processCandidate(lifeStory, jobDescription);
        System.out.println("=== 候选人工作流完成 ===");
        System.out.println("最终简历: " + candidateCv);

        ////////////////// 招聘团队组合工作流 //////////////////////
        // 我们收到包含候选人简历和联系方式的电子邮件。我们进行了电话HR面试。
        // 现在我们通过3个并行评审，然后将结果传入条件流程来决定邀请或拒绝。

        // 1. 为招聘团队工作流创建所有必要的智能体
        HrCvReviewer hrCvReviewer = AgenticServices
                .agentBuilder(HrCvReviewer.class)
                .chatModel(tongYiChatModel)
                .outputKey("hrReview")
                .build();

        ManagerCvReviewer managerCvReviewer = AgenticServices
                .agentBuilder(ManagerCvReviewer.class)
                .chatModel(tongYiChatModel)
                .outputKey("managerReview")
                .build();

        TeamMemberCvReviewer teamMemberCvReviewer = AgenticServices
                .agentBuilder(TeamMemberCvReviewer.class)
                .chatModel(tongYiChatModel)
                .outputKey("teamMemberReview")
                .build();

        EmailAssistant emailAssistant = AgenticServices
                .agentBuilder(EmailAssistant.class)
                .chatModel(tongYiChatModel)
                .tools(new OrganizingTools())
                .build();

        InterviewOrganizer interviewOrganizer = AgenticServices
                .agentBuilder(InterviewOrganizer.class)
                .chatModel(tongYiChatModel)
                .tools(new OrganizingTools())
                .contentRetriever(ragProvider.loadHouseRulesRetriever())
                .build();

        // 2. 创建并行评审工作流
        UntypedAgent parallelReviewWorkflow = AgenticServices
                .parallelBuilder()
                .subAgents(hrCvReviewer, managerCvReviewer, teamMemberCvReviewer)
                .executor(Executors.newFixedThreadPool(3))
                .outputKey("combinedCvReview")
                .output(agenticScope -> {
                    CvReview hrReview = (CvReview) agenticScope.readState("hrReview");
                    CvReview managerReview = (CvReview) agenticScope.readState("managerReview");
                    CvReview teamMemberReview = (CvReview) agenticScope.readState("teamMemberReview");
                    String feedback = String.join("\n",
                            "HR评审: " + hrReview.feedback,
                            "经理评审: " + managerReview.feedback,
                            "团队成员评审: " + teamMemberReview.feedback
                    );
                    double avgScore = (hrReview.score + managerReview.score + teamMemberReview.score) / 3.0;
                    System.out.println("最终平均简历评审分数: " + avgScore + "\n");
                    return new CvReview(avgScore, feedback);
                })
                .build();

        // 3. 创建最终决策的条件工作流
        UntypedAgent decisionWorkflow = AgenticServices
                .conditionalBuilder()
                .subAgents(agenticScope -> ((CvReview) agenticScope.readState("combinedCvReview")).score >= 0.8, interviewOrganizer)
                .subAgents(agenticScope -> ((CvReview) agenticScope.readState("combinedCvReview")).score < 0.8, emailAssistant)
                .build();

        // 4. 创建完整的招聘团队工作流：并行评审 → 决策
        HiringTeamWorkflow hiringTeamWorkflow = AgenticServices
                .sequenceBuilder(HiringTeamWorkflow.class)
                .subAgents(parallelReviewWorkflow, decisionWorkflow)
                .build();

        // 5. 加载输入数据
        String candidateContact = new FileSystemResourceLoader().getResource("classpath:data/candidate_contact.txt").getContentAsString(StandardCharsets.UTF_8);
        String hrRequirements = new FileSystemResourceLoader().getResource("classpath:data/hr_requirements.txt").getContentAsString(StandardCharsets.UTF_8);
        String phoneInterviewNotes = new FileSystemResourceLoader().getResource("classpath:data/phone_interview_notes.txt").getContentAsString(StandardCharsets.UTF_8);

        // 6. 执行招聘团队工作流
        hiringTeamWorkflow.processApplication(candidateCv, jobDescription, hrRequirements, phoneInterviewNotes, candidateContact);
        System.out.println("=== 招聘团队工作流完成 ===");
        System.out.println("并行评审完成并已做出决策");
    }

    @Test
    public void testAgent10() throws IOException {
        // 1. 定义所有子智能体
        HrCvReviewer hrReviewer = AgenticServices.agentBuilder(HrCvReviewer.class)
                .chatModel(tongYiChatModel)
                .outputKey("hrReview")
                .build();
        // 重要：如果我们为多个智能体使用相同的方法名
        // （在此例中：所有评审者都使用'reviewCv'方法），最好为智能体命名，像这样：
        // @Agent(name = "managerReviewer", description = "基于职位描述审查简历，提供反馈和评分")
        ManagerCvReviewer managerReviewer = AgenticServices.agentBuilder(ManagerCvReviewer.class)
                .chatModel(tongYiChatModel)
                .outputKey("managerReview")
                .build();
        TeamMemberCvReviewer teamReviewer = AgenticServices.agentBuilder(TeamMemberCvReviewer.class)
                .chatModel(tongYiChatModel)
                .outputKey("teamMemberReview")
                .build();
        InterviewOrganizer interviewOrganizer = AgenticServices.agentBuilder(InterviewOrganizer.class)
                .chatModel(tongYiChatModel)
                .tools(new OrganizingTools())
                .contentRetriever(ragProvider.loadHouseRulesRetriever())
                .build();
        EmailAssistant emailAssistant = AgenticServices.agentBuilder(EmailAssistant.class)
                .chatModel(tongYiChatModel)
                .tools(new OrganizingTools())
                .build();
        // 2. 构建监督者智能体
        SupervisorAgent hiringSupervisor = AgenticServices.supervisorBuilder()
                .chatModel(tongYiChatModel)
                .subAgents(hrReviewer, managerReviewer, teamReviewer, interviewOrganizer, emailAssistant)
                .contextGenerationStrategy(SupervisorContextStrategy.CHAT_MEMORY_AND_SUMMARIZATION)
                .responseStrategy(SupervisorResponseStrategy.SUMMARY)
                .supervisorContext("始终使用所有可用的评审者。始终用中文回答。调用智能体时，使用纯JSON（无反引号，换行符使用反斜杠+n）。")
                .build();
        // 重要须知：监督者一次调用一个智能体，然后审查其计划以选择下一个调用的智能体
        // 监督者无法并行执行智能体
        // 如果智能体被标记为异步，监督者将覆盖该设置（无异步执行）并发出警告
        // 3. 加载候选人简历和职位描述
        String jobDescription = new FileSystemResourceLoader().getResource("classpath:data/job_description_backend.txt").getContentAsString(StandardCharsets.UTF_8);
        String candidateCv = new FileSystemResourceLoader().getResource("classpath:data/tailored_cv.txt").getContentAsString(StandardCharsets.UTF_8);
        String candidateContact = new FileSystemResourceLoader().getResource("classpath:data/candidate_contact.txt").getContentAsString(StandardCharsets.UTF_8);
        String hrRequirements = new FileSystemResourceLoader().getResource("classpath:data/hr_requirements.txt").getContentAsString(StandardCharsets.UTF_8);
        String phoneInterviewNotes = new FileSystemResourceLoader().getResource("classpath:data/phone_interview_notes.txt").getContentAsString(StandardCharsets.UTF_8);

        // 开始计时
        long start = System.nanoTime();
        // 4. 用自然语言请求调用监督者
        String result = hiringSupervisor.invoke(
                "评估以下候选人：\n" +
                        "候选人简历(candidateCv)：\n" + candidateCv + "\n\n" +
                        "候选人联系方式(candidateContact)：\n" + candidateContact + "\n\n" +
                        "职位描述(jobDescription)：\n" + jobDescription + "\n\n" +
                        "HR要求(hrRequirements)：\n" + hrRequirements + "\n\n" +
                        "电话面试记录(phoneInterviewNotes)：\n" + phoneInterviewNotes
        );
        long end = System.nanoTime();
        double elapsedSeconds = (end - start) / 1_000_000_000.0;
        // 在日志中你会注意到最终调用了'done'智能体，这是监督者完成调用系列的方式

        System.out.println("=== 监督者运行完成，耗时 " + elapsedSeconds + " 秒 ===");
        System.out.println(result);
    }

    /**
     * 这里展示如何在智能体工作流中使用非AI智能体（普通Java操作符）。
     * 非AI智能体只是普通的方法，但可以像其他类型的智能体一样使用。
     * 它们非常适合确定性的操作，如计算、数据转换和聚合，这些操作不需要LLM参与。
     * 将更多步骤外包给非AI智能体，你的工作流将更快、更准确、更经济。
     * 对于需要强制确定性执行的步骤，非AI智能体比工具更受青睐。
     * 在这个例子中，我们希望评审者的综合评分是确定性计算的，而不是由LLM计算。
     * 我们同样基于综合评分确定性地更新数据库中的申请状态。
     *
     * @throws IOException
     */
    @Test
    public void testAgent11() throws IOException {
        // 构建并行评审步骤的AI子智能体
        HrCvReviewer hrReviewer = AgenticServices.agentBuilder(HrCvReviewer.class)
                .chatModel(tongYiChatModel)
                .outputKey("hrReview")
                .build();
        ManagerCvReviewer managerReviewer = AgenticServices.agentBuilder(ManagerCvReviewer.class)
                .chatModel(tongYiChatModel)
                .outputKey("managerReview")
                .build();
        TeamMemberCvReviewer teamReviewer = AgenticServices.agentBuilder(TeamMemberCvReviewer.class)
                .chatModel(tongYiChatModel)
                .outputKey("teamMemberReview")
                .build();
        // 构建组合的并行智能体
        ExecutorService executorService = Executors.newFixedThreadPool(3);
        UntypedAgent parallelReviewWorkflow = AgenticServices
                .parallelBuilder()
                .subAgents(hrReviewer, managerReviewer, teamReviewer)
                .executor(executorService)
                .build();
        // 构建包含非AI智能体的完整工作流
        UntypedAgent collectFeedback = AgenticServices.sequenceBuilder().subAgents(parallelReviewWorkflow, new ScoreAggregator(), new StatusUpdate(),
                        AgenticServices.agentAction(agentScope -> {
                            CvReview review = (CvReview) agentScope.readState("combinedCvReview");
                            agentScope.writeState("scoreAsPercentage", review.score * 100); // 当不同系统的智能体通信时，通常需要输出转换
                        }))
                .outputKey("scoreAsPercentage")
                .build();
        String jobDescription = new FileSystemResourceLoader().getResource("classpath:data/job_description_backend.txt").getContentAsString(StandardCharsets.UTF_8);
        String candidateCv = new FileSystemResourceLoader().getResource("classpath:data/tailored_cv.txt").getContentAsString(StandardCharsets.UTF_8);
        String candidateContact = new FileSystemResourceLoader().getResource("classpath:data/candidate_contact.txt").getContentAsString(StandardCharsets.UTF_8);
        String hrRequirements = new FileSystemResourceLoader().getResource("classpath:data/hr_requirements.txt").getContentAsString(StandardCharsets.UTF_8);
        String phoneInterviewNotes = new FileSystemResourceLoader().getResource("classpath:data/phone_interview_notes.txt").getContentAsString(StandardCharsets.UTF_8);
        Map<String, Object> arguments = Map.of(
                "candidateCv", candidateCv,
                "candidateContact", candidateContact,
                "hrRequirements", hrRequirements,
                "phoneInterviewNotes", phoneInterviewNotes,
                "jobDescription", jobDescription
        );
        // 调用工作流
        double scoreAsPercentage = (double) collectFeedback.invoke(arguments);
        executorService.shutdown();

        System.out.println("=== 百分比形式的评分 ===");
        System.out.println(scoreAsPercentage);
    }

    /**
     * 人机协同
     * https://cloud.tencent.com/developer/article/2626126
     *
     * @throws IOException
     */
    @Test
    public void testAgent12() throws IOException {
        // 创建相关智能体
        InterviewOrganizer interviewOrganizer = AgenticServices.agentBuilder(InterviewOrganizer.class)
                .chatModel(tongYiChatModel)
                .tools(new OrganizingTools())
                .contentRetriever(ragProvider.loadHouseRulesRetriever())
                .build();

        EmailAssistant emailAssistant = AgenticServices.agentBuilder(EmailAssistant.class)
                .chatModel(tongYiChatModel)
                .tools(new OrganizingTools())
                .build();

        HiringDecisionProposer decisionProposer = AgenticServices.agentBuilder(HiringDecisionProposer.class)
                .chatModel(tongYiChatModel)
                .outputKey("modelDecision")
                .build();

        // 定义人工验证环节
        HumanInTheLoop humanValidator = AgenticServices.humanInTheLoopBuilder()
                .description("验证模型提出的招聘决策")
                .inputKey("modelDecision")
                .outputKey("finalDecision") // 由人工检查
                .requestWriter(request -> {
                    System.out.println("AI招聘助手建议: " + request);
                    System.out.println("请确认最终决定。");
                    System.out.println("选项: 邀请现场面试 (I), 拒绝 (R), 暂缓 (H)");
                    System.out.print("> "); // 在实际系统中需要输入验证和错误处理
                })
                .responseReader(() -> new Scanner(System.in).nextLine())
                .build();

        // 将智能体链接成工作流
        UntypedAgent hiringDecisionWorkflow = AgenticServices.sequenceBuilder()
                .subAgents(decisionProposer, humanValidator)
                .outputKey("finalDecision")
                .build();

        // 准备输入参数
        Map<String, Object> input = Map.of(
                "cvReview", new CvReview(0.85,
                        """
                                技术能力强，但缺乏所需的React经验。
                                似乎是快速独立学习者。文化契合度良好。
                                工作许可可能存在潜在问题，但似乎可以解决。
                                薪资期望略高于计划预算。
                                决定继续进行现场面试。
                                """)
        );
        System.out.println(input + "\n");

        // 运行工作流
        String finalDecision = (String) hiringDecisionWorkflow.invoke(input);

        System.out.println("\n=== 人工最终决定 ===");
        System.out.println("(邀请现场面试 (I), 拒绝 (R), 暂缓 (H))\n");
        System.out.println(finalDecision);

        UntypedAgent candidateResponder = AgenticServices
                .conditionalBuilder()
                .subAgents(agenticScope -> finalDecision.contains("I"), interviewOrganizer)
                .subAgents(agenticScope -> finalDecision.contains("R"), emailAssistant)
                .subAgents(agenticScope -> finalDecision.contains("H"), new HoldOnAssist())
                .build();

        String candidateContact = new FileSystemResourceLoader().getResource("classpath:data/candidate_contact.txt").getContentAsString(StandardCharsets.UTF_8);
        String jobDescription = new FileSystemResourceLoader().getResource("classpath:data/job_description_backend.txt").getContentAsString(StandardCharsets.UTF_8);

        Map<String, Object> arguments = Map.of(
                "candidateContact", candidateContact,
                "jobDescription", jobDescription
        );

        // 根据人工最终决定，进行下一步操作
        candidateResponder.invoke(arguments);
    }

    @Test
    public void testAgent13() throws IOException {
        ReActAssistant agent = AgenticServices
                .agentBuilder(ReActAssistant.class)
                .chatModel(tongYiChatModel)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(15))
                .tools(sampleTools)
                .build();

        String[] testQueries = {
                "计算 15 加上 27 等于多少？",
                "北京现在的天气怎么样？",
                "计算半径为5的圆的面积",
                "现在是几点？",
                "计算长方体的体积，长10，宽5，高3",
                "帮我算一下 (25 × 4) ÷ 2 等于多少？",
                "快递单123456,现在到哪了？",
                "我的订单56789,退款到账了没？"
        };

        for (String query : testQueries) {
            System.out.println("问: " + query);
            try {
                String response = agent.chat(query);
                System.out.println("答: " + response);
                System.out.println("-".repeat(50));
                // 避免请求过快
                Thread.sleep(1000);
            } catch (Exception e) {
                System.err.println("查询失败: " + e.getMessage());
            }
        }
    }

    @Test
    public void testAgent14() throws IOException {
        String[] testTasks = {
                "计算 15 加上 27 等于多少？",
                "北京现在的天气怎么样？",
                "计算半径为5的圆的面积",
                "现在是几点？",
                "计算长方体的体积，长10，宽5，高3",
                "帮我算一下 (25 × 4) ÷ 2 等于多少？",
                "快递单123456,现在到哪了？",
                "我的订单56789,退款到账了没？"
        };

        Coordinator coordinator = new Coordinator(tongYiChatModel, sampleTools);

        for (int i = 0; i < testTasks.length; i++) {
            System.out.printf("\n📦 测试用例 %d/%d%n", i + 1, testTasks.length);

            Map<String, Object> result = coordinator.executeTask(testTasks[i]);

            // 打印总结
            System.out.println("\n✅ 任务完成总结:");
            System.out.println("-".repeat(40));
            System.out.println("任务: " + result.get("task"));
            System.out.println("状态: " + result.get("status"));
            System.out.println("耗时: " + calculateDuration(
                    (String) result.get("start_time"),
                    (String) result.get("end_time")
            ));

            if (result.containsKey("execution_results")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> executions =
                        (List<Map<String, Object>>) result.get("execution_results");
                System.out.println("执行步骤数: " + executions.size());
            }

            System.out.println("=".repeat(60));

            // 任务间暂停
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        coordinator.printContext();
    }


    private static String calculateDuration(String start, String end) {
        try {
            LocalDateTime startTime = LocalDateTime.parse(start);
            LocalDateTime endTime = LocalDateTime.parse(end);
            Duration duration = Duration.between(startTime, endTime);
            return String.format("%d秒", duration.getSeconds());
        } catch (Exception e) {
            return "未知";
        }
    }
}
