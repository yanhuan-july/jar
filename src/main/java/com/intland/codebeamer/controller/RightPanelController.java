package com.intland.codebeamer.controller;

import com.intland.codebeamer.manager.TrackerItemManager;
import com.intland.codebeamer.persistence.dto.TrackerItemDto;
import com.intland.codebeamer.persistence.dto.UserDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController               // == @Controller + @ResponseBody on every method
@RequestMapping("/trackers/ajax")
public class RightPanelController {
    @GetMapping("/ping.spr")
    public String ping() {
        return "pong";
    }

    @Autowired
    private TrackerItemManager trackerItemManager;

    @PostMapping(
            value    = "/custom_item.spr",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Map<String,Object> getCustomItemData(
            HttpServletRequest request,
            @RequestParam("itemId") int itemId
    ) {
        // 1) 必须从 session 拿到当前用户
        UserDto user = (UserDto) request.getSession(false).getAttribute("currentUser");
        if (user == null) {
            return Map.of("error", "Session 失效，请重新登录");
        }
        // 2) 载入 TrackerItem
        TrackerItemDto item = trackerItemManager.findById(user, itemId);
        if (item == null) {
            return Map.of("error", "找不到 ID=" + itemId);
        }
        // 3) 封装并返回纯 JSON
        Map<String,Object> result = new HashMap<>();
        result.put("id",          item.getId());
        result.put("name",        item.getName());
        result.put("description", item.getDescription());
        result.put("status",      item.getStatus().getName());
        return result;
    }
}
