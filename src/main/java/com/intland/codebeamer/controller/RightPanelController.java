package com.intland.codebeamer.controller;

import com.intland.codebeamer.manager.TrackerItemManager;
import com.intland.codebeamer.persistence.dto.TrackerItemDto;
import com.intland.codebeamer.persistence.dto.UserDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/trackers/ajax")
public class RightPanelController {

    @Autowired
    private TrackerItemManager trackerItemManager;

    /**
     * AJAX 获取当前 Item 的基本信息。
     * POST /trackers/ajax/custom_item.spr?itemId=123
     * 返回 JSON：{id, name, description, status}
     */
    @PostMapping(
            value    = "/custom_item.spr",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Map<String,Object> getCustomItemData(
            HttpServletRequest request,
            @RequestParam("itemId") int itemId
    ) {
        // —— 1. 从 Session 中取 currentUser ——
        UserDto user = (UserDto) request.getSession(false).getAttribute("currentUser");
        if (user == null) {
            return Collections.singletonMap("error", "Session 失效或未登录");
        }
        // —— 2. 加载 TrackerItemDto ——
        TrackerItemDto item = trackerItemManager.findById(user, itemId);
        if (item == null) {
            return Collections.singletonMap("error", "未找到 ID=" + itemId);
        }
        // —— 3. 封装并返回 JSON ——
        Map<String,Object> result = new HashMap<>();
        result.put("id",          item.getId());
        result.put("name",        item.getName());
        result.put("description", item.getDescription());
        result.put("status",      item.getStatus().getName());
        return result;
    }
}
