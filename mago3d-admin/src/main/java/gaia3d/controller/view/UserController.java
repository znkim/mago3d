package gaia3d.controller.view;

import gaia3d.controller.AuthorizationController;
import gaia3d.domain.Key;
import gaia3d.domain.PageType;
import gaia3d.domain.common.Pagination;
import gaia3d.domain.policy.Policy;
import gaia3d.domain.role.RoleKey;
import gaia3d.domain.user.UserGroup;
import gaia3d.domain.user.UserInfo;
import gaia3d.domain.user.UserSession;
import gaia3d.domain.user.UserStatus;
import gaia3d.security.crypto.Crypt;
import gaia3d.service.PolicyService;
import gaia3d.service.UserGroupService;
import gaia3d.service.UserService;
import gaia3d.support.PasswordSupport;
import gaia3d.support.SQLInjectSupport;
import gaia3d.utils.DateUtils;
import gaia3d.utils.FormatUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.ObjectUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * 사용자
 * @author kimhj
 *
 */
@Slf4j
@Controller
@RequestMapping("/user")
public class UserController implements AuthorizationController {

	@Autowired
	private UserService userService;

	@Autowired
	private UserGroupService userGroupService;

	@Autowired
	private PolicyService policyService;

	/**
	 * 사용자 목록
	 * @param request
	 * @param userInfo
	 * @param pageNo
	 * @param model
	 * @return
	 */
	@GetMapping(value = "/list")
	public String list(HttpServletRequest request, @RequestParam(defaultValue="1") String pageNo, UserInfo userInfo, Model model) {
		log.info("@@ userInfo = {}", userInfo);

		userInfo.setSearchWord(SQLInjectSupport.replaceSqlInection(userInfo.getSearchWord()));
		userInfo.setOrderWord(SQLInjectSupport.replaceSqlInection(userInfo.getOrderWord()));
		
		String roleCheckResult = roleValidate(request);
    	if(roleValidate(request) != null) return roleCheckResult;

    	String today = DateUtils.getToday(FormatUtils.YEAR_MONTH_DAY);
		if(!ObjectUtils.isEmpty(userInfo.getStartDate())) {
			userInfo.setStartDate(userInfo.getStartDate().substring(0, 8) + DateUtils.START_TIME);
		}
		if(!ObjectUtils.isEmpty(userInfo.getEndDate())) {
			userInfo.setEndDate(userInfo.getEndDate().substring(0, 8) + DateUtils.END_TIME);
		}

    	long totalCount = userService.getUserTotalCount(userInfo);
    	Pagination pagination = new Pagination(request.getRequestURI(), getSearchParameters(PageType.LIST, userInfo),
    			totalCount, Long.parseLong(pageNo), userInfo.getListCounter());
    	userInfo.setOffset(pagination.getOffset());
    	userInfo.setLimit(pagination.getPageRows());

		List<UserInfo> userList = new ArrayList<>();
		if(totalCount > 0L) {
			userList = userService.getListUser(userInfo);
		}

		model.addAttribute(pagination);
		model.addAttribute("userList", userList);
		return "/user/list";
	}

	/**
	 * 사용자 상세 정보
	 * @param request
	 * @param userInfo
	 * @param model
	 * @return
	 */
	@GetMapping(value = "/detail")
	public String detail(HttpServletRequest request, UserInfo userInfo, Model model) {
		String listParameters = getSearchParameters(PageType.DETAIL, userInfo);

		userInfo =  userService.getUser(userInfo.getUserId());
		Policy policy = policyService.getPolicy();

		model.addAttribute("policy", policy);
		model.addAttribute("listParameters", listParameters);
		model.addAttribute("userInfo", userInfo);
		return "/user/detail";
	}

	/**
	 * 사용자 등록  페이지 이동
	 */
	@GetMapping(value = "/input")
	public String input(Model model) {
		Policy policy = policyService.getPolicy();
		List<UserGroup> userGroupList = userGroupService.getListUserGroup();

		model.addAttribute("policy", policy);
		model.addAttribute("userGroupList", userGroupList);
		model.addAttribute("userInfo", new UserInfo());
		return "/user/input";
	}

	/**
	 * 사용자 수정  페이지 이동
	 * @param request
	 * @param userId
	 * @param model
	 * @return
	 */
	@GetMapping(value = "/modify")
	public String modify(HttpServletRequest request, @RequestParam String userId, Model model) {
		String roleCheckResult = roleValidate(request);
    	if(roleValidate(request) != null) return roleCheckResult;

        Policy policy = policyService.getPolicy();
        UserInfo userInfo = userService.getUser(userId);
        if(!ObjectUtils.isEmpty(userInfo.getEmail())) {
			userInfo.setEmail(Crypt.decrypt(userInfo.getEmail()));
		}
		List<UserGroup> userGroupList = userGroupService.getListUserGroup();

		String type = request.getParameter("type");
		log.info("@@ type = {}", type);

		model.addAttribute("type", type);
        model.addAttribute("policy", policy);
        model.addAttribute("userInfo", userInfo);
        model.addAttribute("userGroupList", userGroupList);

        return "/user/modify";
	}

    /**
	 * 사용자 삭제
	 * @param userId
	 * @param model
	 * @return
	 */
	@GetMapping(value = "/delete")
	public String delete(@RequestParam("userId") String userId, Model model) {
		// TODO validation 체크 해야 함
		userService.deleteUser(userId);

		return "redirect:/user/list";
	}
	
	/**
	 * 비밀번호 수정
	 * @param model
	 * @return
	 */
	@GetMapping(value = "/modify-password")
	public String modifyPassword(HttpServletRequest request, Model model) {
		
		Policy policy = policyService.getPolicy();

		model.addAttribute("policy", policy);
		model.addAttribute("userInfo", new UserInfo());
		return "/user/modify-password";
	}
	
	/**
	 * 비밀번호 수정
	 * @param request
	 * @param userInfo
	 * @param bindingResult
	 * @param model
	 * @return
	 */
	@PostMapping(value = "/update-password")
	public String updatePassword(HttpServletRequest request, @ModelAttribute("userInfo") UserInfo userInfo, BindingResult bindingResult, Model model) {
		Policy policy = policyService.getPolicy();
		// TODO validator 이용하게 수정해야 함
		
		String errorcode = userValidate(policy, userInfo);
		if(errorcode != null) {
			log.info("@@@@@@@@@@@@@ errcode = {}", errorcode);
			userInfo.setErrorCode(errorcode);
			model.addAttribute("policy", policy);
			return "/user/modify-password";		
		}
		
		UserSession userSession = (UserSession)request.getSession().getAttribute(Key.USER_SESSION.name());
		UserInfo dbUserInfo = userService.getUser(userSession.getUserId());
		if(!PasswordSupport.isEquals(dbUserInfo.getPassword(), userInfo.getPassword())){
			errorcode = "user.password.compare.invalid";
			log.info("@@@@@@@@@@@@@ errcode = {}", errorcode);
			userInfo.setErrorCode(errorcode);
			model.addAttribute("policy", policy);
			return "/user/modify-password";
		}
		
		String encryptPassword = PasswordSupport.encodePassword(userInfo.getNewPassword());
		if(encryptPassword == null) {
			errorcode = "user.password.exception";
			log.info("@@@@@@@@@@@@@ errcode = {}", errorcode);
			userInfo.setErrorCode(errorcode);
			model.addAttribute("policy", policy);
			return "/user/modify-password";
		}
		
		userInfo.setUserId(userSession.getUserId());
		userInfo.setPassword(userInfo.getNewPassword());
		userInfo.setStatus(UserStatus.USE.getValue());
		userService.updatePassword(userInfo);
		
		// 임시 패스워드인 경우 세션을 사용중 상태로 변경
		if(UserStatus.TEMP_PASSWORD == UserStatus.findBy(userSession.getStatus())) {
			userSession.setStatus(UserStatus.USE.getValue());
		}
	
		return "redirect:/main/index";
	}
	
	/**
	 * validation 체크
	 * @param userInfo
	 * @return
	 */
	private String userValidate(Policy policy, UserInfo userInfo) {
		return PasswordSupport.validateUserPassword(policy, userInfo);
	}

	/**
	 * 검색 조건
	 * @param userInfo
	 * @return
	 */
	private String getSearchParameters(PageType pageType, UserInfo userInfo) {
		StringBuffer buffer = new StringBuffer(userInfo.getParameters());
		boolean isListPage = true;
		if(pageType == PageType.MODIFY || pageType == PageType.DETAIL) {
			isListPage = false;
		}

		if(!ObjectUtils.isEmpty(userInfo.getStatus())) {
			buffer.append("&status=").append(userInfo.getStatus());
		}

		return buffer.toString();
	}

	private String roleValidate(HttpServletRequest request) {
    	UserSession userSession = (UserSession)request.getSession().getAttribute(Key.USER_SESSION.name());
		int httpStatusCode = getRoleStatusCode(userSession.getUserGroupId(), RoleKey.ADMIN_USER_MANAGE.name());
		if(httpStatusCode > 200) {
			log.info("@@ httpStatusCode = {}", httpStatusCode);
			request.setAttribute("httpStatusCode", httpStatusCode);
			return "/error/error";
		}

		return null;
    }
}
