package gaia3d.controller.view;

import gaia3d.config.PropertiesConfig;
import gaia3d.domain.ApprovalType;
import gaia3d.domain.SharingType;
import gaia3d.domain.SigninType;
import gaia3d.domain.SignupType;
import gaia3d.domain.cache.CacheManager;
import gaia3d.domain.data.DataGroup;
import gaia3d.domain.policy.Policy;
import gaia3d.domain.user.UserGroupType;
import gaia3d.domain.user.UserInfo;
import gaia3d.domain.user.UserStatus;
import gaia3d.service.DataGroupService;
import gaia3d.service.UserService;
import gaia3d.support.PasswordSupport;
import gaia3d.utils.FileUtils;
import gaia3d.utils.LocaleUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sign up 처리
 * 
 * @author jeongdae
 */
@Slf4j
@Controller
@RequestMapping("/sign")
public class SignupController {

	@Autowired
	private UserService userService;
	@Autowired
	private PropertiesConfig propertiesConfig;
	@Autowired
	private DataGroupService dataGroupService;
	@Autowired
	private MessageSource messageSource;

	/**
	 * Sign up 페이지
	 * @param request
	 * @param model
	 * @return
	 */
	@GetMapping("/signup")
	public String signup(HttpServletRequest request, Model model) {
		Policy policy = CacheManager.getPolicy();
		log.info("@@ policy = {}", policy);

		UserInfo userInfo = new UserInfo();

		model.addAttribute("signupForm", userInfo);
		model.addAttribute("policy", policy);
		model.addAttribute("contentCacheVersion", policy.getContentCacheVersion());

		return "/sign/signup";
	}

	/**
	 * Social Sign up 페이지
	 * @param request
	 * @param model
	 * @return
	 */
	@GetMapping("/social-signup")
	public String socialSignup(HttpServletRequest request, Model model) {
		Policy policy = CacheManager.getPolicy();
		log.info("@@ policy = {}", policy);

		UserInfo userInfo = new UserInfo();

		model.addAttribute("signupForm", userInfo);
		model.addAttribute("policy", policy);
		model.addAttribute("contentCacheVersion", policy.getContentCacheVersion());

		return "/sign/social-signup";
	}

	/**
	 * Sign up 처리
	 * @param request
	 * @param signupForm
	 * @param bindingResult
	 * @param model
	 * @return
	 */
	@PostMapping(value = "/process-signup")
	public String processSignup(HttpServletRequest request, @Valid @ModelAttribute("signupForm") UserInfo signupForm, BindingResult bindingResult, Model model) {
		log.info("@@ signupForm = {}", signupForm);
		Policy policy = CacheManager.getPolicy();

		Boolean userIdDuplication = userService.isUserIdDuplication(signupForm);
		Boolean emailDuplication = userService.isEmailDuplication(signupForm.getEmail());
		log.info("@@ userIdDuplication = {}, emailDuplication = {}", userIdDuplication, emailDuplication);
		if(userIdDuplication || emailDuplication) {
			if(userIdDuplication) signupForm.setErrorCode("user.id.duplication");
			else signupForm.setErrorCode("user.email.duplication");

			signupForm.setPassword(null);
			signupForm.setPasswordConfirm(null);
			model.addAttribute("signupForm", signupForm);
			model.addAttribute("policy", policy);
			return "/sign/signup";
		}

		String errorcode = validate(policy, signupForm);
		if(errorcode != null) {
			log.info("@@errorcode = {}", errorcode);
			signupForm.setErrorCode(errorcode);
			signupForm.setPassword(null);
			signupForm.setPasswordConfirm(null);
			model.addAttribute("signupForm", signupForm);
			model.addAttribute("policy", policy);

			return "/sign/signup";
		}

		signupForm.setSigninType(SigninType.BASIC.toString());
		signupForm.setSignupType(SignupType.BASIC.toString());
		signupForm.setUserGroupId(UserGroupType.USER.getValue());
		if(ApprovalType.AUTO == ApprovalType.valueOf(policy.getSignupType().toUpperCase())) {
			signupForm.setStatus(UserStatus.USE.getValue());
		} else {
			signupForm.setStatus(UserStatus.WAITING_APPROVAL.getValue());
		}
		userService.insertUser(signupForm);

		try {
			createUserDataGroupDirectory(LocaleUtils.getUserLocale(request), signupForm.getUserId(), signupForm.getUserId() + "/basic/");
		} catch (Exception e) {
			FileUtils.deleteFileRecursive(propertiesConfig.getUserDataServicePath() + signupForm.getUserId() + "/basic/");
			userService.deleteUser(signupForm.getUserId());
			signupForm.setErrorCode("user.data.group.directory.make.fail");
			signupForm.setPassword(null);
			signupForm.setPasswordConfirm(null);
			model.addAttribute("signupForm", signupForm);
			return "/sign/signup";
		}

		return "redirect:/sign/signup-complete";
	}

	/**
	 * Sign up 처리
	 * @param request
	 * @param signupForm
	 * @param bindingResult
	 * @param model
	 * @return
	 */
	@PostMapping(value = "/process-social-signup")
	public String processSocialSignup(HttpServletRequest request, @Valid @ModelAttribute("signupForm") UserInfo signupForm, BindingResult bindingResult, Model model) {
		log.info("@@ signupForm = {}", signupForm);
		Policy policy = CacheManager.getPolicy();

		Boolean userIdDuplication = userService.isUserIdDuplication(signupForm);
		Boolean emailDuplication = userService.isEmailDuplication(signupForm.getEmail());
		log.info("@@ userIdDuplication = {}, emailDuplication = {}", userIdDuplication, emailDuplication);
		if(userIdDuplication || emailDuplication) {
			if(userIdDuplication) signupForm.setErrorCode("user.id.duplication");
			else signupForm.setErrorCode("user.email.duplication");

			signupForm.setPassword(null);
			signupForm.setPasswordConfirm(null);
			model.addAttribute("signupForm", signupForm);
			model.addAttribute("policy", policy);
			return "/sign/social-signup";
		}

		if(!isValidEmail(signupForm.getEmail())){
			log.info("@@ invalid email = {}", signupForm.getEmail());
			signupForm.setErrorCode("user.email.invalid");
			signupForm.setPassword(null);
			signupForm.setPasswordConfirm(null);
			model.addAttribute("signupForm", signupForm);
			model.addAttribute("policy", policy);
			return "/sign/social-signup";
		}

		signupForm.setSigninType(SigninType.SOCIAL.toString());
		signupForm.setSignupType(SignupType.SOCIAL.toString());
		signupForm.setUserGroupId(UserGroupType.USER.getValue());
		if(ApprovalType.AUTO == ApprovalType.valueOf(policy.getSignupType().toUpperCase())) {
			signupForm.setStatus(UserStatus.USE.getValue());
		} else {
			signupForm.setStatus(UserStatus.WAITING_APPROVAL.getValue());
		}
		userService.insertUser(signupForm);

		try {
			createUserDataGroupDirectory(LocaleUtils.getUserLocale(request), signupForm.getUserId(), signupForm.getUserId() + "/basic/");
		} catch (Exception e) {
			FileUtils.deleteFileRecursive(propertiesConfig.getUserDataServicePath() + signupForm.getUserId() + "/basic/");
			userService.deleteUser(signupForm.getUserId());
			signupForm.setErrorCode("user.data.group.directory.make.fail");
			signupForm.setPassword(null);
			signupForm.setPasswordConfirm(null);
			model.addAttribute("signupForm", signupForm);
			return "/sign/social-signup";
		}

		return "redirect:/sign/signup-complete";
	}

	/**
	 * Sign up 완료
	 * @param request
	 * @param model
	 * @return
	 */
	@GetMapping("/signup-complete")
	public String signupComplete(HttpServletRequest request, Model model) {
		Policy policy = CacheManager.getPolicy();

		model.addAttribute("signupType", policy.getSignupType().toUpperCase());

		return "/sign/signup-complete";
	}

	/**
	 * validation 체크
	 * @param userInfo
	 * @return
	 */
	private String validate(Policy policy, UserInfo userInfo) {
		String errorCode = PasswordSupport.validatePassword(policy, userInfo);
		if(errorCode != null) return errorCode;

		if(!isValidEmail(userInfo.getEmail())){
			log.info(userInfo.getEmail());
			return "user.email.invalid";
		}

		return null;
	}

	private boolean isValidEmail(String email) {
		boolean err = false;
		String regex = "^[_a-z0-9-]+(.[_a-z0-9-]+)*@(?:\\w+\\.)+\\w+$";
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(email);
		if(m.matches()) {
			err = true;
		}
		return err;
	}

	private void createUserDataGroupDirectory(Locale locale, String userId, String dataGroupPath) throws Exception {
		// 데이터 업로딩 경로 생성
		DataGroup dataGroup = new DataGroup();
		dataGroup.setUserId(userId);
		dataGroup.setDataGroupKey("basic");
		dataGroup.setDataGroupName(messageSource.getMessage("common.basic", null, locale));
		dataGroup.setDataGroupPath(propertiesConfig.getUserDataServicePath() + dataGroupPath);
		dataGroup.setSharing(SharingType.PUBLIC.name().toLowerCase());
		dataGroup.setMetainfo("{\"isPhysical\": false}");
		dataGroupService.insertBasicDataGroup(dataGroup);
		if(!FileUtils.makeDirectoryByPath(propertiesConfig.getUserDataServiceDir(), dataGroupPath)) {
			dataGroupService.deleteDataGroup(dataGroup);
			throw new Exception("user.data.group.directory.make.fail");
		}
	}
}
