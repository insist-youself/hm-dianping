package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;

import static com.hmdp.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Override
    public Result sendCode(String phone, HttpSession session) {
        // 1. 校验手机号是否有效
        if(RegexUtils.isPhoneInvalid(phone)) {
            // 2. 如果不符合返回错误信息
            return Result.fail("手机号格式错误!!");
        }
        // 3. 符合，生成对应的验证码
        String code = RandomUtil.randomNumbers(6);
        // 4. 保存验证码到session中
        session.setAttribute("code", code);
        // 5. 发送验证码
        log.debug("发送短信验证码成功，验证码为：{}", code);
        // 6. 返回ok
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        // 1. 校验手机号
        String phone = loginForm.getPhone();
        // 2. 校验验证码
        if(RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号格式错误!!");
        }
        // 3. 验证码不一致报错
        Object cacheCode = session.getAttribute("code");
        String code = loginForm.getCode();
        if(cacheCode == null || !cacheCode.toString().equals(code)){
            return Result.fail("验证码错误！");
        }
        // 4. 一致，根据手机号查询用户
        User user = query().eq("phone", phone).one();
        // 5. 用户不存在，则创建新用户并保存
        if(user == null) {
            user = createUserWithPhone(phone);
        }
        // 6. 将用户保存到session中
        session.setAttribute("user", BeanUtil.copyProperties(user, UserDTO.class));
        return Result.ok();
    }

    private User createUserWithPhone(String phone) {
        // 1. 创建用户
        User user = new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
        // 2. 保存用户
        save(user);
        return user;
    }
}
