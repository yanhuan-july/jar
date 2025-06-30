package com.intland.codebeamer.controller;

import com.intland.codebeamer.manager.TrackerItemManager;
import com.intland.codebeamer.persistence.dto.TrackerItemDto;
import com.intland.codebeamer.persistence.dto.UserDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController                        // = @Controller + @ResponseBody
@RequestMapping("/trackers/ajax")       // 统一前缀
public class RightPanelController {

    @Autowired
    private TrackerItemManager trackerItemManager;

    @PostMapping(
            value    = "/custom_item_v2.spr",        // ★ 接口相对路径
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Map<String, Object> getCustomItemData(
            @AuthenticationPrincipal UserDto user,     // 当前登录用户由 Spring 注入
            @RequestParam("itemId") int itemId) {

        /* 1) 会话真的失效时返回错误（前端就会弹“请重新登录”） */
        if (user == null) {
            return Map.of("error", "Session 失效，请重新登录");
        }

        /* 2) 查找测试用例（Tracker Item） */
        TrackerItemDto item = trackerItemManager.findById(user, itemId);
        if (item == null) {
            return Map.of("error", "找不到 ID=" + itemId);
        }



        /* 3) 只把需要的字段返回给前端 */
        Map<String, Object> result = new HashMap<>();
        result.put("id",          item.getId());
        result.put("name",        item.getName());
        result.put("description", item.getDescription());
        //result.put("status",      item.getStatus().getName());
        return result;
    }
}
