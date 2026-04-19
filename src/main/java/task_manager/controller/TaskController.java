package task_manager.controller;

import task_manager.entity.Task;
import task_manager.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
@CrossOrigin(origins = "*")
public class TaskController {

    private static final String AI_SERVICE_BASE = "http://localhost:5001";

    @Autowired
    private TaskRepository taskRepository;

    private final RestTemplate restTemplate = new RestTemplate();

    // ─────────────────────────────────────────────
    // 查询所有任务（支持关键字搜索）
    // ─────────────────────────────────────────────
    @GetMapping
    public List<Task> getAllTasks(@RequestParam(required = false) String keyword) {
        if (keyword != null && !keyword.isEmpty()) {
            return taskRepository.searchByKeyword(keyword);
        }
        return taskRepository.findAll();
    }

    // ─────────────────────────────────────────────
    // 根据 ID 查询单个任务
    // ─────────────────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<Task> getTaskById(@PathVariable String id) {
        return taskRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ─────────────────────────────────────────────
    // 手动创建任务
    // ─────────────────────────────────────────────
    @PostMapping
    public ResponseEntity<Task> createTask(@RequestBody Task task) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskRepository.save(task));
    }

    // ─────────────────────────────────────────────
    // 修改任务
    // ─────────────────────────────────────────────
    @PutMapping("/{id}")
    public ResponseEntity<Task> updateTask(@PathVariable String id,
                                           @RequestBody Task taskDetails) {
        return taskRepository.findById(id).map(task -> {
            task.setTitle(taskDetails.getTitle());
            task.setDescription(taskDetails.getDescription());
            task.setStatus(taskDetails.getStatus());
            task.setPriority(taskDetails.getPriority());
            task.setTags(taskDetails.getTags());
            return ResponseEntity.ok(taskRepository.save(task));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ─────────────────────────────────────────────
    // 删除任务
    // ─────────────────────────────────────────────
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable String id) {
        if (!taskRepository.existsById(id)) return ResponseEntity.notFound().build();
        taskRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ─────────────────────────────────────────────
    // 批量删除
    // ─────────────────────────────────────────────
    @PostMapping("/bulk-delete")
    public ResponseEntity<Void> bulkDeleteTasks(@RequestBody List<String> ids) {
        taskRepository.deleteAllById(ids);
        return ResponseEntity.noContent().build();
    }


    //功能1：AI根据自然语言自动创建任务
    @PostMapping("/ai-generate")
    public ResponseEntity<?> aiGenerateTask(@RequestBody Map<String, String> body) {

        String prompt = body.get("prompt");
        if (prompt == null || prompt.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "prompt 不能为空"));
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> req =
                    new HttpEntity<>(Map.of("prompt", prompt), headers);

            ResponseEntity<Map> aiResponse =
                    restTemplate.postForEntity(AI_SERVICE_BASE + "/generate", req, Map.class);

            Map<?, ?> aiData = aiResponse.getBody();
            if (aiData == null || aiData.containsKey("error")) {
                String msg = aiData != null ? (String) aiData.get("error") : "AI 服务无响应";
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                        .body(Map.of("error", "AI 服务错误: " + msg));
            }

            Task task = new Task();
            task.setTitle(getString(aiData, "title", prompt.substring(0, Math.min(20, prompt.length()))));
            task.setDescription(getString(aiData, "description", ""));
            task.setStatus(Task.Status.pending);
            try {
                task.setPriority(Task.Priority.valueOf(getString(aiData, "priority", "medium")));
            } catch (IllegalArgumentException e) {
                task.setPriority(Task.Priority.medium);
            }
            Object tags = aiData.get("tags");
            if (tags instanceof List<?> tagList) {
                task.setTags(tagList.stream()
                        .filter(t -> t instanceof String)
                        .map(t -> (String) t).toList());
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(taskRepository.save(task));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "无法连接 AI 服务，请确认 Python 服务已启动。详情: " + e.getMessage()));
        }
    }


    //功能2：AI 任务分解，按执行顺序分配优先级（前1/3高、中1/3中、后1/3低），批量存库，返回子任务列表

    @PostMapping("/ai-decompose")
    public ResponseEntity<?> aiDecomposeTask(@RequestBody Map<String, String> body) {

        String prompt = body.get("prompt");
        if (prompt == null || prompt.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "prompt 不能为空"));
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> req =
                    new HttpEntity<>(Map.of("prompt", prompt), headers);

            ResponseEntity<List> aiResponse =
                    restTemplate.postForEntity(AI_SERVICE_BASE + "/decompose", req, List.class);

            List<?> subtasksRaw = aiResponse.getBody();
            if (subtasksRaw == null || subtasksRaw.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                        .body(Map.of("error", "AI 未能分解出任何子任务"));
            }

            List<Task> savedTasks = subtasksRaw.stream()
                    .filter(item -> item instanceof Map)
                    .map(item -> {
                        Map<?, ?> m = (Map<?, ?>) item;
                        Task t = new Task();
                        t.setTitle(getString(m, "title", "未命名子任务"));
                        t.setDescription(getString(m, "description", ""));
                        t.setStatus(Task.Status.pending);
                        try {
                            t.setPriority(Task.Priority.valueOf(getString(m, "priority", "medium")));
                        } catch (IllegalArgumentException e) {
                            t.setPriority(Task.Priority.medium);
                        }
                        Object tags = m.get("tags");
                        if (tags instanceof List<?> tagList) {
                            t.setTags(tagList.stream()
                                    .filter(tag -> tag instanceof String)
                                    .map(tag -> (String) tag).toList());
                        }
                        return t;
                    })
                    .map(taskRepository::save)
                    .toList();

            return ResponseEntity.status(HttpStatus.CREATED).body(savedTasks);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "无法连接 AI 服务，请确认 Python 服务已启动。详情: " + e.getMessage()));
        }
    }
    //任务3：使用AI智能检索任务
    @PostMapping("/ai-search")
    public ResponseEntity<?> aiSearchTasks(@RequestBody Map<String, String> body) {

        String prompt = body.get("prompt");
        if (prompt == null || prompt.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "prompt 不能为空"));
        }

        try {
            // 1. 调用 Python 服务提炼关键词
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> req =
                    new HttpEntity<>(Map.of("prompt", prompt), headers);

            ResponseEntity<Map> aiResponse =
                    restTemplate.postForEntity(AI_SERVICE_BASE + "/smart-search", req, Map.class);

            Map<?, ?> aiData = aiResponse.getBody();
            if (aiData == null || aiData.containsKey("error")) {
                String msg = aiData != null ? (String) aiData.get("error") : "AI 服务无响应";
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                        .body(Map.of("error", "AI 服务错误: " + msg));
            }

            // 2. 用提炼出的关键词搜索数据库
            String keyword = getString(aiData, "keyword", prompt.substring(0, Math.min(10, prompt.length())));
            List<Task> results = taskRepository.searchByKeyword(keyword);

            // 3. 把关键词一并返回，方便前端展示"搜索词：xxx"
            return ResponseEntity.ok(Map.of(
                    "keyword", keyword,
                    "results", results
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "无法连接 AI 服务，请确认 Python 服务已启动。详情: " + e.getMessage()));
        }
    }
    // ─────────────────────────────────────────────
    // 工具方法：安全地从 Map 取字符串
    // ─────────────────────────────────────────────
    private String getString(Map<?, ?> map, String key, String defaultVal) {
        Object val = map.get(key);
        return (val instanceof String s && !s.isBlank()) ? s : defaultVal;
    }
}