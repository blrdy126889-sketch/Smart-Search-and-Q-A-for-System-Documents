package com.docqa.category;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.docqa.category.entity.BizCategory;
import com.docqa.category.mapper.BizCategoryMapper;
import com.docqa.common.api.R;
import com.docqa.common.exception.BizException;
import com.docqa.common.log.OpLog;
import com.docqa.common.util.SecurityUtils;
import com.docqa.document.entity.BizDocument;
import com.docqa.document.mapper.BizDocumentMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 分类目录管理：树查询 / 增删改 / 分类数据授权
 */
@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final BizCategoryMapper categoryMapper;
    private final BizDocumentMapper documentMapper;

    @GetMapping("/tree")
    public R<List<Map<String, Object>>> tree() {
        List<BizCategory> all = categoryMapper.selectList(
                new LambdaQueryWrapper<BizCategory>().orderByAsc(BizCategory::getSortOrder));
        boolean isAdmin = SecurityUtils.roleCodes().contains("ADMIN");
        var roleIds = SecurityUtils.roleIds();
        List<BizCategory> visible = isAdmin ? all : all.stream()
                .filter(c -> categoryMapper.selectPermRoleIds(c.getId()).stream().anyMatch(roleIds::contains))
                .toList();
        return R.ok(buildTree(visible, 0L));
    }

    private List<Map<String, Object>> buildTree(List<BizCategory> all, Long parentId) {
        List<Map<String, Object>> tree = new ArrayList<>();
        for (BizCategory c : all) {
            if (!parentId.equals(c.getParentId())) continue;
            Map<String, Object> node = new HashMap<>();
            node.put("id", c.getId());
            node.put("parentId", c.getParentId());
            node.put("categoryName", c.getCategoryName());
            node.put("sortOrder", c.getSortOrder());
            node.put("docCount", c.getDocCount());
            node.put("authorizedRoleIds", categoryMapper.selectPermRoleIds(c.getId()));
            node.put("children", buildTree(all, c.getId()));
            tree.add(node);
        }
        return tree;
    }

    @Data
    public static class CategoryDTO {
        private Long parentId;
        private String categoryName;
        private Integer sortOrder;
    }

    @PostMapping
    @OpLog(module = "分类管理", operation = "新增分类")
    public R<Void> create(@RequestBody CategoryDTO dto) {
        SecurityUtils.checkPerm("doc:category:edit");
        if (dto.getCategoryName() == null || dto.getCategoryName().isBlank()) {
            throw new BizException("分类名称不能为空");
        }
        BizCategory parent = null;
        if (dto.getParentId() != null && dto.getParentId() > 0) {
            parent = categoryMapper.selectById(dto.getParentId());
            if (parent == null) throw new BizException("父分类不存在");
        }
        BizCategory category = new BizCategory();
        category.setParentId(parent == null ? 0L : parent.getId());
        category.setAncestors(parent == null ? "0" : parent.getAncestors() + "," + parent.getId());
        category.setCategoryName(dto.getCategoryName());
        category.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        categoryMapper.insert(category);
        for (long rid = 1; rid <= 4; rid++) categoryMapper.insertCategoryPerm(category.getId(), rid);
        return R.ok();
    }

    @PutMapping("/{id}")
    @OpLog(module = "分类管理", operation = "修改分类")
    public R<Void> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        SecurityUtils.checkPerm("doc:category:edit");
        BizCategory category = categoryMapper.selectById(id);
        if (category == null) throw new BizException("分类不存在");
        var wrapper = new LambdaUpdateWrapper<BizCategory>().eq(BizCategory::getId, id);
        if (body.containsKey("categoryName")) wrapper.set(BizCategory::getCategoryName, body.get("categoryName"));
        if (body.containsKey("sortOrder")) wrapper.set(BizCategory::getSortOrder, ((Number) body.get("sortOrder")).intValue());
        if (body.containsKey("parentId")) {
            Long newParentId = ((Number) body.get("parentId")).longValue();
            if (newParentId.equals(id)) throw new BizException("父分类不能是自身");
            BizCategory newParent = newParentId == 0 ? null : categoryMapper.selectById(newParentId);
            if (newParentId != 0 && newParent == null) throw new BizException("目标父分类不存在");
            if (newParent != null && newParent.getAncestors() != null
                    && (newParent.getAncestors() + ",").startsWith(category.getAncestors() + ",")) {
                throw new BizException("不能将分类移动到其子分类下");
            }
            wrapper.set(BizCategory::getParentId, newParentId);
            String ancestors = newParent == null ? "0" : newParent.getAncestors() + "," + newParent.getId();
            wrapper.set(BizCategory::getAncestors, ancestors);
            for (BizCategory child : categoryMapper.selectList(new LambdaQueryWrapper<BizCategory>()
                    .likeRight(BizCategory::getAncestors, category.getAncestors() + "," + id))) {
                String newChildPath = child.getAncestors().replaceFirst(
                        category.getAncestors() + "," + id, ancestors + "," + id);
                categoryMapper.update(null, new LambdaUpdateWrapper<BizCategory>()
                        .eq(BizCategory::getId, child.getId()).set(BizCategory::getAncestors, newChildPath));
            }
        }
        categoryMapper.update(null, wrapper);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @OpLog(module = "分类管理", operation = "删除分类")
    @Transactional
    public R<Void> delete(@PathVariable Long id) {
        SecurityUtils.checkPerm("doc:category:edit");
        Long childCount = categoryMapper.selectCount(
                new LambdaQueryWrapper<BizCategory>().eq(BizCategory::getParentId, id));
        if (childCount > 0) throw new BizException("存在子分类，请先删除子分类");
        Long docCount = documentMapper.selectCount(new LambdaQueryWrapper<BizDocument>()
                .eq(BizDocument::getCategoryId, id));
        if (docCount > 0) throw new BizException("分类下存在文档，无法删除");
        categoryMapper.deleteById(id);
        categoryMapper.deleteCategoryPerms(id);
        return R.ok();
    }

    @PutMapping("/{id}/perms")
    @OpLog(module = "分类管理", operation = "分类授权")
    @Transactional
    public R<Void> assignPerms(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        SecurityUtils.checkPerm("doc:category:edit");
        categoryMapper.deleteCategoryPerms(id);
        @SuppressWarnings("unchecked")
        List<Number> roleIds = (List<Number>) body.getOrDefault("roleIds", List.of());
        for (Number rid : roleIds) categoryMapper.insertCategoryPerm(id, rid.longValue());
        return R.ok();
    }
}
