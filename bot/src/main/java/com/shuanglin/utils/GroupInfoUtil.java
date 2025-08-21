package com.shuanglin.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shuanglin.dao.model.Model;
import com.shuanglin.dao.GroupInfo;
import com.shuanglin.dao.model.ModelInfo;
import com.shuanglin.dao.model.ModelsRepository;
import com.shuanglin.dao.SenderInfo;
import com.shuanglin.framework.bus.event.GroupMessageEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class GroupInfoUtil {
	private final static String GROUP_SENDER_STAFF = "group_sender_staff_";

	private final static String GROUP_INFO_STAFF = "group_info_staff_";

	@Resource(name = "senderInfoRedisTemplate")
	private final RedisTemplate<String, Map<String, SenderInfo>> senderInfoRedisTemplate;

	@Resource(name = "groupInfoRedisTemplate")
	private final RedisTemplate<String, Object> groupInfoRedisTemplate;


	private final ModelsRepository modelsRepository;

	public void publishModel(Model model) {
		Model modelByModelName = modelsRepository.getModelByModelName(model.getModelName());
		if (modelByModelName != null) {
			log.info("已存在模型");
			return;
		}
		modelsRepository.save(model);
		senderInfoRedisTemplate.delete(GROUP_SENDER_STAFF);
		groupInfoRedisTemplate.delete(GROUP_INFO_STAFF);
	}

	public void switchModel(SenderInfo senderInfo, String modelName) {
		Model modelByModelName = modelsRepository.getModelByModelName(modelName);
		if (modelByModelName != null) {
			Map<String, SenderInfo> senderInfoMap = senderInfoRedisTemplate.opsForValue().get(GROUP_SENDER_STAFF + senderInfo.getGroupId());
			ModelInfo modelInfo = senderInfo.getModelInfo();
			modelInfo.setModelName(modelByModelName.getModelName());
			senderInfoMap.put(senderInfo.getUserId(), senderInfo);
			senderInfoMap.remove(null);
			senderInfoRedisTemplate.opsForValue().set(GROUP_SENDER_STAFF + senderInfo.getGroupId(), senderInfoMap);
		}
	}

	/**
	 * 检查本群是否开启模型
	 *
	 * @param groupMessageEvent 组消息事件
	 * @param selectModel       选择型号
	 * @return boolean
	 */
	public boolean checkModelPermission(GroupMessageEvent groupMessageEvent, String selectModel) {
		GroupInfo groupInfo = getGroupInfo(groupMessageEvent);
		List<String> groupActiveModels = groupInfo.getModelInfo().getActiveModels();
		if (groupActiveModels.contains(selectModel)) {
			return true;
		}
		return false;
	}


	public SenderInfo getGroupSenderInfo(GroupMessageEvent groupMessageEvent) {
		Map<String, SenderInfo> senderInfoMap = senderInfoRedisTemplate.opsForValue().get(GROUP_SENDER_STAFF + groupMessageEvent.getGroupId());
		if (senderInfoMap == null) {
			senderInfoMap = new HashMap<>();
			SenderInfo.getInstance().setGroupId(groupMessageEvent.getGroupId());
			senderInfoMap.put(groupMessageEvent.getGroupId(), SenderInfo.getInstance());
			senderInfoRedisTemplate.opsForValue().set(GROUP_SENDER_STAFF + groupMessageEvent.getGroupId(), senderInfoMap);
			return SenderInfo.getInstance();
		}
		if (senderInfoMap.get(String.valueOf(groupMessageEvent.getUserId())) == null) {
			List<Model> actives = modelsRepository.getModelsByIsActive("true");
			SenderInfo.getInstance().setGroupId(groupMessageEvent.getGroupId());
			SenderInfo.getInstance().setUserId(String.valueOf(groupMessageEvent.getUserId()));
			SenderInfo.getInstance().setModelInfo(ModelInfo.builder()
					.activeModels(actives.stream().map(Model::getModelName).collect(Collectors.toList()))
					.modelName(actives.get(0).getModelName())
					.build());
			senderInfoMap.put(String.valueOf(groupMessageEvent.getUserId()), SenderInfo.getInstance());
			senderInfoMap.remove(null);
			senderInfoRedisTemplate.opsForValue().set(GROUP_SENDER_STAFF + groupMessageEvent.getGroupId(), senderInfoMap);
			return SenderInfo.getInstance();
		}
		return senderInfoMap.get(String.valueOf(groupMessageEvent.getUserId()));
	}

	public GroupInfo getGroupInfo(GroupMessageEvent groupMessageEvent) {
		GroupInfo groupInfo = new ObjectMapper().convertValue(groupInfoRedisTemplate.opsForHash().get(GROUP_INFO_STAFF, groupMessageEvent.getGroupId()), GroupInfo.class);
		if (groupInfo == null) {
			List<String> modelsByActive = modelsRepository.getModelsByIsActive("true").stream().map(Model::getModelName).collect(Collectors.toList());
			groupInfo = GroupInfo.getInstance();
			groupInfo.setGroupId(groupMessageEvent.getGroupId());
			groupInfo.setModelInfo(ModelInfo.builder().activeModels(modelsByActive).modelName(modelsByActive.isEmpty() ? "1" : modelsByActive.get(0)).build());
			GroupInfo.getInstance().setGroupId(groupMessageEvent.getGroupId());
			groupInfoRedisTemplate.opsForHash().put(GROUP_INFO_STAFF, groupMessageEvent.getGroupId(), GroupInfo.getInstance());
		}
		return groupInfo;
	}

}
