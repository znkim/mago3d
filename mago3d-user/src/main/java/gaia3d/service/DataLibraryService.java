package gaia3d.service;

import gaia3d.domain.datalibrary.DataLibrary;

import java.util.List;

/**
 * 데이터 라이브러리 관리
 * @author jeongdae
 *
 */
public interface DataLibraryService {


	/**
	 * 데이터 라이브러리 목록
	 * @param dataLibrary
	 * @return
	 */
	List<DataLibrary> getListDataLibrary(DataLibrary dataLibrary);


	/**
	 * 데이터 라이브러리 정보 취득
	 * @param dataLibrary
	 * @return
	 */
	DataLibrary getDataLibrary(DataLibrary dataLibrary);
}
