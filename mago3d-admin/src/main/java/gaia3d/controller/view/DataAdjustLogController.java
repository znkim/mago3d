package gaia3d.controller.view;

import gaia3d.domain.Key;
import gaia3d.domain.PageType;
import gaia3d.domain.common.Pagination;
import gaia3d.domain.data.DataAdjustLog;
import gaia3d.domain.data.DataGroup;
import gaia3d.domain.user.UserSession;
import gaia3d.service.DataAdjustLogService;
import gaia3d.service.DataGroupService;
import gaia3d.support.SQLInjectSupport;
import gaia3d.utils.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 데이터 geometry 변경 이력
 * @author jeongdae
 *
 */
@Slf4j
@Controller
@RequestMapping("/data-adjust-log")
public class DataAdjustLogController {
	
	@Autowired
	private DataGroupService dataGroupService;
	@Autowired
	private DataAdjustLogService dataAdjustLogService;
	
	/**
	 * 데이터 geometry 변경 이력 목록
	 * @param locale
	 * @param request
	 * @param dataAdjustLog
	 * @param pageNo
	 * @param model
	 * @return
	 */
	@GetMapping(value = "/list")
	public String list(Locale locale, HttpServletRequest request, DataAdjustLog dataAdjustLog, @RequestParam(defaultValue="1") String pageNo, Model model) {
		dataAdjustLog.setSearchWord(SQLInjectSupport.replaceSqlInection(dataAdjustLog.getSearchWord()));
		dataAdjustLog.setOrderWord(SQLInjectSupport.replaceSqlInection(dataAdjustLog.getOrderWord()));
		
		log.info("@@ dataInfoAdjustLog = {}", dataAdjustLog);
		UserSession userSession = (UserSession)request.getSession().getAttribute(Key.USER_SESSION.name());
		
		DataGroup dataGroup = new DataGroup();
		dataGroup.setUserId(userSession.getUserId());
		List<DataGroup> dataGroupList = dataGroupService.getListDataGroup(dataGroup);
		
		if(!ObjectUtils.isEmpty(dataAdjustLog.getStartDate())) {
			dataAdjustLog.setStartDate(dataAdjustLog.getStartDate().substring(0, 8) + DateUtils.START_TIME);
		}
		if(!ObjectUtils.isEmpty(dataAdjustLog.getEndDate())) {
			dataAdjustLog.setEndDate(dataAdjustLog.getEndDate().substring(0, 8) + DateUtils.END_TIME);
		}

		long totalCount = dataAdjustLogService.getDataAdjustLogTotalCount(dataAdjustLog);
		Pagination pagination = new Pagination(request.getRequestURI(), getSearchParameters(PageType.LIST, dataAdjustLog),
				totalCount, Long.parseLong(pageNo), dataAdjustLog.getListCounter());
		dataAdjustLog.setOffset(pagination.getOffset());
		dataAdjustLog.setLimit(pagination.getPageRows());

		List<DataAdjustLog> dataAdjustLogList = new ArrayList<>();
		if(totalCount > 0l) {
			dataAdjustLogList = dataAdjustLogService.getListDataAdjustLog(dataAdjustLog);
		}

		model.addAttribute(pagination);
		model.addAttribute("dataGroupList", dataGroupList);
		model.addAttribute("dataAdjustLogList", dataAdjustLogList);
		return "/data-adjust-log/list";
	}

	/**
	 * 검색 조건
	 * @param pageType
	 * @param dataAdjustLog
	 * @return
	 */
	private String getSearchParameters(PageType pageType, DataAdjustLog dataAdjustLog) {
		return dataAdjustLog.getParameters();
	}
}