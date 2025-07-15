package com.shuanglin.aop.enums;

/**
 * 事件常量
 *  用于存储Event的所有常量 枚举
 *
 * @author lin
 * @date 2025/07/15
 */
public class EventConstant {
	public static final String CMD_DEFAULT_STAFF_VALUE = "#";

	public enum MessageEventType {
		/**
		 * 群消息
		 */
		GROUP_MESSAGE,

		/**
		 * 私聊消息
		 */
		PRIVATE_MESSAGE,

		/**
		 * 讨论组消息
		 */
		DISCUSSION_MESSAGE,

		/**
		 * 好友添加请求
		 */
		FRIEND_REQUEST,

		/**
		 * 群成员变更事件
		 */
		GROUP_MEMBER_CHANGE,

		/**
		 * 群文件上传事件
		 */
		GROUP_FILE_UPLOAD,

		/**
		 * 其他类型的事件
		 */
		OTHER
	}
}
