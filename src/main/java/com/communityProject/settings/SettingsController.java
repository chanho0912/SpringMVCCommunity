package com.communityProject.settings;

import com.communityProject.account.AccountService;
import com.communityProject.account.CurrentUser;
import com.communityProject.domain.Account;
import com.communityProject.domain.Tag;
import com.communityProject.settings.form.NicknameForm;
import com.communityProject.settings.form.Notifications;
import com.communityProject.settings.form.PasswordForm;
import com.communityProject.settings.form.Profile;
import com.communityProject.settings.validator.NicknameFormValidator;
import com.communityProject.settings.validator.PasswordFormValidator;
import com.communityProject.tags.TagForm;
import com.communityProject.tags.TagRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class SettingsController {
    static final String SETTINGS_PROFILE_VIEW_NAME = "settings/profile";
    static final String SETTINGS_PROFILE_URL = "/settings/profile";

    static final String SETTINGS_PASSWORD_VIEW_NAME = "settings/password";
    static final String SETTINGS_PASSWORD_URL = "/settings/password";

    static final String SETTINGS_NOTIFICATION_VIEW_NAME = "settings/notifications";
    static final String SETTINGS_NOTIFICATION_URL = "/settings/notifications";

    static final String SETTINGS_TAGS_VIEW_NAME = "settings/tags";
    static final String SETTINGS_TAGS_URL = "/settings/tags";

    static final String SETTINGS_ACCOUNT_VIEW_NAME = "settings/account";
    static final String SETTINGS_ACCOUNT_URL = "/settings/account";

    private final AccountService accountService;
    private final TagRepository tagRepository;
    private final ModelMapper modelMapper;
    private final ObjectMapper objectMapper;

    private final NicknameFormValidator nicknameFormValidator;

    @InitBinder("passwordForm")
    public void passwordFormInitBinder(WebDataBinder webDataBinder) {
        webDataBinder.addValidators(new PasswordFormValidator());
    }

    @InitBinder("nicknameForm")
    public void nicknameFormInitBinder(WebDataBinder webDataBinder) {
        webDataBinder.addValidators(nicknameFormValidator);
    }



    @GetMapping(SETTINGS_PROFILE_URL)
    public String updateProfileForm(@CurrentUser Account account, Model model) {
        model.addAttribute(account);
        model.addAttribute(modelMapper.map(account, Profile.class));

        return SETTINGS_PROFILE_VIEW_NAME;
    }

    @PostMapping(SETTINGS_PROFILE_URL)
    public String updateProfile(@CurrentUser Account account, @Valid Profile profile,
                                Errors errors, Model model, RedirectAttributes attributes) {
        if(errors.hasErrors()) {
            model.addAttribute(account);
            return SETTINGS_PROFILE_VIEW_NAME;
        }

        accountService.updateProfile(account, profile);
        attributes.addFlashAttribute("message", "프로필을 수정했습니다.");
        return "redirect:"+SETTINGS_PROFILE_URL;
    }

    @GetMapping(SETTINGS_PASSWORD_URL)
    public String updatePasswordForm(@CurrentUser Account account, Model model) {
        model.addAttribute(account);
        model.addAttribute(new PasswordForm());
        return SETTINGS_PASSWORD_VIEW_NAME;
    }

    @PostMapping(SETTINGS_PASSWORD_URL)
    public String updatePassword(@CurrentUser Account account, @Valid PasswordForm passwordForm,
                                 Errors errors, Model model, RedirectAttributes attributes) {
        if(errors.hasErrors()) {
            model.addAttribute(account);
            return SETTINGS_PASSWORD_VIEW_NAME;
        }

        accountService.updatePassword(account, passwordForm.getNewPassword());
        attributes.addFlashAttribute("message", "패스워드를 변경하였습니다.");
        return "redirect:" + SETTINGS_PASSWORD_URL;
    }

    @GetMapping(SETTINGS_TAGS_URL)
    public String updateTags(@CurrentUser Account account, Model model) throws JsonProcessingException {
        model.addAttribute(account);
        Set<Tag> tags = accountService.getTags(account);
        List<String> allTags = tagRepository.findAll().stream().map(Tag::getTitle).collect(Collectors.toList());

        model.addAttribute("tags", tags.stream().map(Tag::getTitle).collect(Collectors.toList()));
        model.addAttribute("whitelist", objectMapper.writeValueAsString(allTags));
        return SETTINGS_TAGS_VIEW_NAME;
    }

    @PostMapping(SETTINGS_TAGS_URL + "/add")
    @ResponseBody
    public ResponseEntity addTag(@CurrentUser Account account, @RequestBody TagForm tagForm) {
        String title = tagForm.getTagTitle();
        Tag tag = tagRepository.findByTitle(title).orElseGet(
                ()-> tagRepository.save(Tag.builder()
                        .title(tagForm.getTagTitle())
                        .build()
                ));

        accountService.addTag(account, tag);
        return ResponseEntity.ok().build();
    }

    @PostMapping(SETTINGS_TAGS_URL + "/remove")
    @ResponseBody
    public ResponseEntity removeTag(@CurrentUser Account account, @RequestBody TagForm tagForm) {
        String title = tagForm.getTagTitle();
        Optional<Tag> tag = tagRepository.findByTitle(title);
        if(tag.isEmpty()) return ResponseEntity.badRequest().build();

        accountService.removeTag(account, tag.get());
        return ResponseEntity.ok().build();
    }

    @GetMapping(SETTINGS_NOTIFICATION_URL)
    public String updateNotificationsForm(@CurrentUser Account account, Model model) {
        model.addAttribute(account);
        model.addAttribute(modelMapper.map(account, Notifications.class));
        return SETTINGS_NOTIFICATION_VIEW_NAME;
    }

    @PostMapping(SETTINGS_NOTIFICATION_URL)
    public String updateNotifications(@CurrentUser Account account, @Valid Notifications notifications,
                                      Errors errors, Model model, RedirectAttributes redirectAttributes) {
        if(errors.hasErrors()) {
            model.addAttribute(account);
            return SETTINGS_NOTIFICATION_VIEW_NAME;
        }

        accountService.updateNotifications(account, notifications);
        redirectAttributes.addFlashAttribute("message", "알림 설정을 변경했습니다.");
        return "redirect:" + SETTINGS_NOTIFICATION_URL;
    }

    @GetMapping(SETTINGS_ACCOUNT_URL)
    public String updateAccountForm(@CurrentUser Account account, Model model) {
        model.addAttribute(account);
        model.addAttribute(modelMapper.map(account, NicknameForm.class));
        return SETTINGS_ACCOUNT_VIEW_NAME;
    }

    @PostMapping(SETTINGS_ACCOUNT_URL)
    public String updateAccount(@CurrentUser Account account, @Valid NicknameForm nicknameForm, Errors errors,
                                Model model, RedirectAttributes attributes) {
        if(errors.hasErrors()) {
            model.addAttribute(account);
            return SETTINGS_ACCOUNT_VIEW_NAME;
        }

        accountService.updateNickname(account, nicknameForm.getNickname());
        attributes.addFlashAttribute("message", "닉네임 수정을 완료했습니다.");
        return "redirect:" + SETTINGS_ACCOUNT_URL;
    }
}
