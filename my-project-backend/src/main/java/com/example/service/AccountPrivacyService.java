package com.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.entity.dto.AccountPrivacy;
import com.example.entity.vo.request.ChangePasswordVO;
import com.example.entity.vo.request.SavePrivacyVO;
import com.example.utils.Const;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;

public interface AccountPrivacyService extends IService<AccountPrivacy> {
    void savePrivacy(int id, SavePrivacyVO vo);
    AccountPrivacy  accountPrivacy(int id);
}
