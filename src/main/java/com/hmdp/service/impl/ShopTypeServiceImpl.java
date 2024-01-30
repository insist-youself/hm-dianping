package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstants;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private IShopTypeService typeService;

    @Override
    public Result queryList() {
        /*List<ShopType> typeList = typeService
                .query().orderByAsc("sort").list();*/
        //1. 查询redis缓存中的商铺分类信息
        ListOperations<String, String> listOperations = stringRedisTemplate.opsForList();
        Long size = listOperations.size(RedisConstants.CACHE_SHOPTYPE_KEY);
        List<String> typeStringList = listOperations.range(RedisConstants.CACHE_SHOPTYPE_KEY, 0, size - 1);

        // 2。存在则直接返回
        if (typeStringList != null && !typeStringList.isEmpty()) {
            List<ShopType> typeList = new ArrayList<>();
            typeStringList.forEach(type -> typeList.add(JSONUtil.toBean(type, ShopType.class)));
            return Result.ok(typeList);
        }
        // 3. 不存在，则查询数据库
        List<ShopType> typeList = typeService.query().orderByAsc("sort").list();
        // 4. 不存在，返回错误信息
        if(typeList == null || typeList.isEmpty()) {
            return Result.fail("未查询到商铺分类信息！");
        }
        // 5. 存在，则将数据加入到redis缓存中
        typeList.forEach(type ->
                stringRedisTemplate.opsForList().rightPushAll(RedisConstants.CACHE_SHOPTYPE_KEY, JSONUtil.toJsonStr(type)));;
        // 6.返回
        return Result.ok(typeList);
    }
}
