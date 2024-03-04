package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.entity.dto.AccountPrivacy;
import com.example.entity.vo.request.SavePrivacyVO;
import com.example.mapper.AccountPrivacyMapper;
import com.example.service.AccountPrivacyService;
import com.example.service.AccountService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AccountPrivacyServiceImpl extends ServiceImpl<AccountPrivacyMapper, AccountPrivacy> implements AccountPrivacyService {
    /**
     * 保存用户的隐私选择，一次只更改一个隐私类型
     *
     * @param id 用户id
     * @param vo 用户隐私选择
     */
    @Override
    @Transactional
    public void savePrivacy(int id, SavePrivacyVO vo) {
        AccountPrivacy privacy= Optional.ofNullable(this.getById(id)).orElse(new AccountPrivacy(id));
        boolean status=vo.isStatus();
        switch(vo.getType()){
            case "phone" -> privacy.setPhone(status);
            case "qq" -> privacy.setQq(status);
            case "wx" -> privacy.setWx(status);
            case "email" -> privacy.setEmail(status);
            case "gender" -> privacy.setGender(status);
        }
        this.saveOrUpdate(privacy);
    }

    @Override
    public AccountPrivacy  accountPrivacy(int id){
        return Optional.ofNullable(this.getById(id)).orElse(new AccountPrivacy(id));
    }
}
