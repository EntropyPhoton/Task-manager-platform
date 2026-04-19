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

    private static final String AI_SERVICE_URL = "http://localhost:5001/generate";

    @Autowired
    private TaskRepository taskRepository;

    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping
    public List<Task> getAllTasks(@RequestParam(required = false) String keyword) {
        if (keyword != null && !keyword.isEmpty()) {
            return taskRepository.searchByKeyword(keyword);
        }
        return taskRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Task> getTaskById(@PathVariable String id) {
        return taskRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Task> createTask(@RequestBody Task task) {
        Task saved = taskRepository.save(task);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }


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


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable String id) {
        if (!taskRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        taskRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }


    @PostMapping("/bulk-delete")
    public ResponseEntity<Void> bulkDeleteTasks(@RequestBody List<String> ids) {
        taskRepository.deleteAllById(ids);
        return ResponseEntity.noContent().build();
    }
    @PostMapping("/ai-generate")
    public ResponseEntity<?> aiGenerateTask(@RequestBody Map<String, String> body) {

        String prompt = body.get("prompt");
        if (prompt == null || prompt.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "prompt 不能为空"));
        }

        try {
            // 1. 构建请求，转发给 Python AI 服务
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> request =
                    new HttpEntity<>(Map.of("prompt", prompt), headers);

            ResponseEntity<Map> aiResponse =
                    restTemplate.postForEntity(AI_SERVICE_URL, request, Map.class);

            // 2. 检查 Python 服务是否返回了错误
            Map<?, ?> aiData = aiResponse.getBody();
            if (aiData == null || aiData.containsKey("error")) {
                String errMsg = aiData != null ? (String) aiData.get("error") : "AI 服务无响应";
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                        .body(Map.of("error", "AI 服务错误: " + errMsg));
            }

            // 3. 将 AI 返回数据映射成 Task 实体
            Task task = new Task();
            task.setTitle(getString(aiData, "title", prompt.substring(0, Math.min(20, prompt.length()))));
            task.setDescription(getString(aiData, "description", ""));

            // 解析 priority 枚举（兜底 medium）
            try {
                task.setPriority(Task.Priority.valueOf(getString(aiData, "priority", "medium")));
            } catch (IllegalArgumentException e) {
                task.setPriority(Task.Priority.medium);
            }

            // 新任务状态固定为 pending
            task.setStatus(Task.Status.pending);

            // 解析 tags 列表
            Object tagsObj = aiData.get("tags");
            if (tagsObj instanceof List<?> tagList) {
                task.setTags(tagList.stream()
                        .filter(t -> t instanceof String)
                        .map(t -> (String) t)
                        .toList());
            }

            // 4. 存库并返回
            Task saved = taskRepository.save(task);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);

        } catch (Exception e) {
            // Python 服务未启动或网络异常
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "无法连接 AI 服务，请确认 Python 服务已在 5001 端口启动。详情: " + e.getMessage()));
        }
    }

    private String getString(Map<?, ?> map, String key, String defaultVal) {
        Object val = map.get(key);
        return (val instanceof String s && !s.isBlank()) ? s : defaultVal;
    }

}