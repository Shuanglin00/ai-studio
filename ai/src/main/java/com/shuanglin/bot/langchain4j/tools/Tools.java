/*
package org.example.wxjr.framework.ai.langchain4j.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import org.example.wxjr.entity.nameScreeningCase.NssNameScreeningCase;
import org.example.wxjr.entity.nameScreeningSubject.NssNameScreeningSubject;
import org.example.wxjr.mapper.nameScreeningCase.NssNameScreeningCaseMapper;
import org.example.wxjr.mapper.nameScreeningSubject.NssNameScreeningSubjectMapper;
import org.example.wxjr.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

*/
/**
 * @author un
 * @date 2025/6/22 22:02
 *//*

@Component
public class SubjectTools {

	private static final Logger log = LoggerFactory.getLogger(SubjectTools.class);
	@Autowired
	private NssNameScreeningSubjectMapper nssNameScreeningSubjectMapper;

	@Autowired
	private NssNameScreeningCaseMapper nssNameScreeningCaseMapper;

	@Tool(name = "根据subjectId查询Subject数据",value = "根据subjectId查询一条Subject数据，无需访问向量存储")
	public String selectSubjectById(@ToolMemoryId int memoryId, @P("编号")String subjectId){
		NssNameScreeningSubject nssNameScreeningSubject = nssNameScreeningSubjectMapper.selectById(subjectId);
		return  JsonUtils.toJsonString(nssNameScreeningSubject);
	}

	@Tool(name = "插入一条subject数据",value = "根据参数，插入一条subject的数据，无需访问向量存储")
	public String insertSubject(@ToolMemoryId int memoryId, NssNameScreeningSubject nameScreeningSubject){
		try {
			nameScreeningSubject.setNameScreeningSubjectId(null);
			log.info("正在插入一条subject数据:{}",nameScreeningSubject);
			nssNameScreeningSubjectMapper.insert(nameScreeningSubject);
		} catch (Exception e) {
			log.error("插入数据异常：{}",e.getMessage());
			return "插入数据失败了："+e.getMessage();
		}
		return "插入成功，插入的数据："+JsonUtils.toJsonString(nameScreeningSubject);
	}

	@Tool(name = "插入案例",value = "根据参数进行数据库当中插入案例，无需访问向量存储")
	public String insertCase(@ToolMemoryId int memoryId,  NssNameScreeningCase nameScreeningCase){
		try {
			nameScreeningCase.setNameScreeningCaseId(null);
			log.info("插入一条case数据:{}",nameScreeningCase);
			nssNameScreeningCaseMapper.insert(nameScreeningCase);
		} catch (Exception e) {
			log.error("插入数据异常：{}",e.getMessage());
			return "插入数据失败了："+e.getMessage();
		}
		return "插入成功，插入的数据："+JsonUtils.toJsonString(nameScreeningCase);
	}

	@Tool(name = "修改案例", value = "根据参数修改案例，无需访问向量存储")
	public String updateSubject(@ToolMemoryId int memoryId, NssNameScreeningSubject nameScreeningSubject){
		try {
			log.info("修改一条subject数据:{}",nameScreeningSubject);
			nssNameScreeningSubjectMapper.updateById(nameScreeningSubject);
		} catch (Exception e) {
			log.error("修改数据异常：{}",e.getMessage());
			return "修改数据失败了："+e.getMessage();
		}
		return "修改成功，修改的数据是:" + JsonUtils.toJsonString(nameScreeningSubject);
	}

	@Tool(name = "修改一条case数据", value = "根据参数修改一条case数据，无需访问向量存储")
	public String updateCase(@ToolMemoryId int memoryId, NssNameScreeningCase nameScreeningCase){
		try {
			log.info("修改一条case数据:{}",nameScreeningCase);
			nssNameScreeningCaseMapper.updateById(nameScreeningCase);
		} catch (Exception e) {
			log.error("修改数据异常：{}",e.getMessage());
			return "修改数据失败了："+e.getMessage();
		}
		return "修改成功，修改的数据是:" + JsonUtils.toJsonString(nameScreeningCase);
	}
}*/
