package com.intland.codebeamer.controller;

import com.intland.codebeamer.persistence.dao.TrackerDao;
import com.intland.codebeamer.persistence.dao.impl.TrackerDaoImpl;
import com.intland.codebeamer.persistence.dao.TrackerItemDao;
import com.intland.codebeamer.persistence.dao.impl.TrackerItemDaoImpl;
import com.intland.codebeamer.persistence.dao.AssociationDao;
import com.intland.codebeamer.persistence.dao.impl.AssociationDaoImpl;
import com.intland.codebeamer.persistence.dto.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

@RestController
@RequestMapping("/trackers/ajax")
public class TestCaseController {
    private static final Logger logger = LogManager.getLogger(TestCaseController.class);

    private final TrackerDao trackerDao = TrackerDaoImpl.getInstance();
    private final TrackerItemDao trackerItemDao = TrackerItemDaoImpl.getInstance();
    private final AssociationDao associationDao = AssociationDaoImpl.getInstance();

    @PostMapping(
            value = "/get_available_test_cases.spr",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Map<String, Object> getAvailableTestCases(
            @AuthenticationPrincipal UserDto user,
            @RequestParam("itemId") int requirementId) {

        // 1. 检查会话
        if (user == null) {
            return Map.of("error", "Session 失效，请重新登录");
        }

        // 2. 获取需求项
        TrackerItemDto requirement = trackerItemDao.findById(user, requirementId);
        if (requirement == null) {
            return Map.of("error", "找不到需求项：ID=" + requirementId);
        }

        // 3. 验证是否为需求类型
        if (!isRequirementType(requirement)) {
            return Map.of("error", "该项不是需求类型");
        }

        try {
            // 4. 获取需求所属的项目ID
            Integer projectId = requirement.getTracker().getProject().getId();

            // 5. 找到项目中所有test case类型的tracker
            List<TrackerDto> testCaseTrackers = trackerDao.findByProjectAndTypes(
                    user,
                    projectId,
                    Collections.singleton(TrackerTypeDto.TESTCASE)
            );

            // 6. 获取这些tracker中的所有items
            List<Map<String, Object>> testCases = new ArrayList<>();
            for (TrackerDto tracker : testCaseTrackers) {
                List<TrackerItemDto> items = trackerItemDao.findByTrackers(
                        user,
                        Collections.singleton(tracker.getId()),
                        null
                );
                if (items != null) {
                    for (TrackerItemDto testCase : items) {
                        Map<String, Object> testCaseInfo = new HashMap<>();
                        testCaseInfo.put("id", testCase.getId());
                        testCaseInfo.put("name", testCase.getName());
                        testCaseInfo.put("description", testCase.getDescription());
                        testCaseInfo.put("trackerId", testCase.getTracker().getId());
                        testCaseInfo.put("trackerName", testCase.getTracker().getName());
                        testCases.add(testCaseInfo);
                    }
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("requirementId", requirementId);
            result.put("requirementName", requirement.getName());
            result.put("testCases", testCases);
            return result;

        } catch (Exception e) {
            logger.error("Error getting test cases for requirement: " + requirementId, e);
            return Map.of("error", "获取测试用例失败：" + e.getMessage());
        }
    }

    @PostMapping(
            value = "/create_test_case_association.spr",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Map<String, Object> createAssociation(
            @AuthenticationPrincipal UserDto user,
            @RequestParam("requirementId") Integer requirementId,
            @RequestParam("testCaseId") Integer testCaseId,
            @RequestParam(value = "associationType", defaultValue = "verifies") String associationType) {
        
        // 1. 检查会话
        if (user == null) {
            return Map.of("error", "Session 失效，请重新登录");
        }

        try {
            // 2. 获取需求item
            TrackerItemDto requirement = trackerItemDao.findById(user, requirementId);
            if (requirement == null) {
                return Map.of("error", "需求不存在");
            }

            // 3. 验证是否为需求类型
            if (!isRequirementType(requirement)) {
                return Map.of("error", "该项不是需求类型");
            }

            // 4. 获取测试用例item
            TrackerItemDto testCase = trackerItemDao.findById(user, testCaseId);
            if (testCase == null) {
                return Map.of("error", "测试用例不存在");
            }

            // 5. 验证是否为测试用例类型
            if (!isTestCaseType(testCase)) {
                return Map.of("error", "所选项不是测试用例类型");
            }

            if (!isValidAssociationType(associationType)) {
                return Map.of("error", "无效的关联类型");
            }

            // 6. 创建关联
            AssociationDto<TrackerItemDto, TrackerItemDto> association = new AssociationDto<>(requirement, testCase);
            association.setTypeId(getAssociationTypeId(associationType));
            association.setSubmitter(user);
            association.setDescription("");
            association.setDescriptionFormat("W");
            associationDao.create(association);

            return Map.of("success", true, "message", "关联创建成功");
        } catch (Exception e) {
            logger.error("Error creating association", e);
            return Map.of("error", "创建关联失败：" + e.getMessage());
        }
    }

    private boolean isValidAssociationType(String associationType) {
        if (StringUtils.isBlank(associationType)) {
            return false;
        }
        // 这里可以添加更多的关联类型验证
        return "verifies".equals(associationType) ||
                "verified_by".equals(associationType) ||
                "depends".equals(associationType) ||
                "depends_on".equals(associationType);
    }

    private int getAssociationTypeId(String associationType) {
        return switch (associationType.toLowerCase()) {
            case "verifies" -> 1;
            case "verified_by" -> 2;
            case "depends" -> 3;
            case "depends_on" -> 4;
            default -> 1; // 默认为 verifies
        };
    }

    private boolean isRequirementType(TrackerItemDto item) {
        return item != null && item.getTracker() != null && 
               TrackerTypeDto.REQUIREMENT.equals(item.getTracker().getType().getId());
    }

    private boolean isTestCaseType(TrackerItemDto item) {
        return item != null && item.getTracker() != null && 
               TrackerTypeDto.TESTCASE.equals(item.getTracker().getType().getId());
    }
}