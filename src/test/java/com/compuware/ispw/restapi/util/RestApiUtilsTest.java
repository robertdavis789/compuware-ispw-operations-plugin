package com.compuware.ispw.restapi.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import com.compuware.ispw.restapi.util.RestApiUtils;

/**
 * Test case for RestApiUtils
 * 
 * @author Sam Zhou
 *
 */
public class RestApiUtilsTest {

	private static Logger logger = Logger.getLogger(RestApiUtilsTest.class);

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testListQueryParms() {
		String contextPath = "/ispw/{srid}/assignments/{assignmentId}/tasks/deploy?level={level}&mname={mname}&mtype={mtype}";
		List<String> queryParams = RestApiUtils.listQueryParams(contextPath);
		logger.info("queryParms=" + queryParams);

		assertEquals(queryParams.size(), 3);
		assertTrue(queryParams.contains("mname"));
		assertTrue(queryParams.contains("level"));
		assertTrue(queryParams.contains("mtype"));
	}

	@Test
	public void testCleanContextPath() {
		String contextPath = "/ispw/{srid}/assignments/{assignmentId}/tasks/deploy?level=DEV1&mname={mname}&mtype={mtype}";

		contextPath = RestApiUtils.cleanContextPath(contextPath);
		logger.info("contextPath=" + contextPath);
		
		assertEquals(contextPath, "/ispw/{srid}/assignments/{assignmentId}/tasks/deploy?level=DEV1");
	}
}
