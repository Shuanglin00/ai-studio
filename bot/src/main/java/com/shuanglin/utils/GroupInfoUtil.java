package com.shuanglin.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shuanglin.dbModel.info.GroupInfo;
import com.shuanglin.dbModel.info.ModelInfo;
import com.shuanglin.dbModel.info.SenderInfo;
import com.shuanglin.dbModel.models.ModelsRepository;
import com.shuanglin.dbModel.permission.PermissionRepository;
import com.shuanglin.framework.bus.event.GroupMessageEvent;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class GroupInfoUtil {
	private final static String GROUP_SENDER_STAFF = "group_sender_staff_";

	private final static String GROUP_INFO_STAFF = "group_info_staff_";

	@Resource(name = "senderInfoRedisTemplate")
	private RedisTemplate<String, Map<String, SenderInfo>> senderInfoRedisTemplate;

	@Resource(name = "groupInfoRedisTemplate")
	private RedisTemplate<String, Object> groupInfoRedisTemplate;

	private PermissionRepository permissionRepository;

	private ModelsRepository modelsRepository;

	public boolean checkModelPermission(GroupMessageEvent groupMessageEvent, String selectModel) {
		SenderInfo senderInfo = getGroupSenderInfo(groupMessageEvent);
		GroupInfo groupInfo = getGroupInfo(groupMessageEvent);
		List<String> groupActiveModels = groupInfo.getModelInfo().getActiveModels();
		if (groupActiveModels.contains(selectModel)) {
			return true;
		}
		return false;
	}


	public SenderInfo getGroupSenderInfo(GroupMessageEvent groupMessageEvent) {
		Map<String, SenderInfo> senderInfoMap = senderInfoRedisTemplate.opsForValue().get(GROUP_SENDER_STAFF + groupMessageEvent.getGroupId());
		GroupInfo groupInfo = getGroupInfo(groupMessageEvent);
		if (senderInfoMap == null) {
			senderInfoMap = new HashMap<>();
			SenderInfo.getInstance().setGroupId(groupMessageEvent.getGroupId());
			senderInfoMap.put(groupMessageEvent.getGroupId(), SenderInfo.getInstance());
			senderInfoRedisTemplate.opsForValue().set(GROUP_SENDER_STAFF + groupMessageEvent.getGroupId(), senderInfoMap);
			return SenderInfo.getInstance();
		}
		if (senderInfoMap.get(groupMessageEvent.getSender().getUserId()) == null) {
			SenderInfo.getInstance().setGroupId(groupMessageEvent.getGroupId());
			SenderInfo.getInstance().setUserId(groupMessageEvent.getSender().getUserId());
			SenderInfo.getInstance().setModelInfo(ModelInfo.builder()
					.activeModels(groupInfo.getModelInfo().getActiveModels())
					.useModel(groupInfo.getModelInfo().getUseModel())
					.build());
			senderInfoMap.put(groupMessageEvent.getSender().getUserId(), SenderInfo.getInstance());
			senderInfoRedisTemplate.opsForValue().set(GROUP_SENDER_STAFF + groupMessageEvent.getGroupId(), senderInfoMap);
			return SenderInfo.getInstance();
		}
		return senderInfoMap.get(groupMessageEvent.getSender().getUserId());
	}

	@NotNull
	private GroupInfo getGroupInfo(GroupMessageEvent groupMessageEvent) {
		GroupInfo groupInfo = new ObjectMapper().convertValue(groupInfoRedisTemplate.opsForHash().get(GROUP_INFO_STAFF, groupMessageEvent.getGroupId()), GroupInfo.class);
		if (groupInfo == null) {
			GroupInfo.getInstance().setGroupId(groupMessageEvent.getGroupId());
			groupInfo = GroupInfo.getInstance();
			groupInfoRedisTemplate.opsForHash().put(GROUP_INFO_STAFF, groupMessageEvent.getGroupId(), GroupInfo.getInstance());
		}
		return groupInfo;
	}

}
