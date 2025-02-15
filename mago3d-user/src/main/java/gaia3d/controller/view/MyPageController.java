package gaia3d.controller.view;

import gaia3d.config.PropertiesConfig;
import gaia3d.domain.Key;
import gaia3d.domain.PageType;
import gaia3d.domain.cache.CacheManager;
import gaia3d.domain.membership.MembershipUsage;
import gaia3d.domain.policy.Policy;
import gaia3d.domain.uploaddata.UploadData;
import gaia3d.domain.user.UserInfo;
import gaia3d.domain.user.UserSession;
import gaia3d.service.MembershipService;
import gaia3d.service.UserService;
import gaia3d.support.RoleSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * MyPage
 * @author jeongdae
 *
 */
@Slf4j
@Controller
@RequestMapping("/mypage")
public class MyPageController {
	
	@Autowired
	private MembershipService membershipService;
	@Autowired
	private PropertiesConfig propertiesConfig;
	@Autowired
	private UserService userService;
	
	/**
	 * 사용자 정보 수정
	 * @param model
	 * @return
	 */
	@GetMapping(value = "/user-modify")
	public String userModify(HttpServletRequest request, Model model) {
		
		UserSession userSession = (UserSession)request.getSession().getAttribute(Key.USER_SESSION.name());
		Policy policy = CacheManager.getPolicy();

		UserInfo userInfo = userService.getUser(userSession.getUserId());

		model.addAttribute("policy", policy);
		model.addAttribute("userInfo", userInfo);

		return "/mypage/user-modify";
	}

	/**
	 * TODO 멤버십 이름이나 key를 uk로 사용해야 함
	 * 멤버십 수정
	 * @param model
	 * @return
	 */
	@GetMapping(value = "/membership")
	public String membership(HttpServletRequest request, Model model) {

		UserSession userSession = (UserSession) request.getSession().getAttribute(Key.USER_SESSION.name());

		MembershipUsage membershipUsage = membershipService.getMembershipUsageByUserId(userSession.getUserId());
		model.addAttribute("membershipUsage", membershipUsage);

		return "/mypage/membership";
	}

	/**
	 * 검색 조건
	 * @param pageType
	 * @param uploadData
	 * @return
	 */
	private String getSearchParameters(PageType pageType, UploadData uploadData) {
		StringBuilder builder = new StringBuilder(uploadData.getParameters());
		boolean isListPage = true;
		if(pageType == PageType.MODIFY || pageType == PageType.DETAIL) {
			isListPage = false;
		}
		
//		if(!isListPage) {
//			builder.append("pageNo=" + request.getParameter("pageNo"));
//			builder.append("&");
//			builder.append("list_count=" + uploadData.getList_counter());
//		}
		
		return builder.toString();
	}
	
	private String roleValidator(HttpServletRequest request, Integer userGroupId, String roleName) {
		List<String> userGroupRoleKeyList = CacheManager.getUserGroupRoleKeyList(userGroupId);
        if(!RoleSupport.isUserGroupRoleValid(userGroupRoleKeyList, roleName)) {
			log.info("---- Role 이 존재하지 않습니다. 확인 하세요. ");
			request.setAttribute("httpStatusCode", 403);
			return "/error/error";
		}
		return null;
	}
}