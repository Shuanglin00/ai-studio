package com.shuanglin.aop.vo;


import java.util.ArrayList;
import java.util.List;
import com.shuanglin.aop.vo.Segment.*;
import com.shuanglin.aop.vo.Segment.data.*;
import com.shuanglin.aop.vo.Segment.data.ShareData;


import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.function.Consumer;

/**
 * 代表一个群消息对象，它由一个目标群组ID和一条消息链组成。
 * <p>
 * 此类为不可变对象，请使用其内部的静态 {@link Builder} 来进行构建。
 * <p>
 * <b>使用示例:</b>
 * <pre>{@code
 * GroupMessage message = GroupMessage.builder()
 *     .groupId(123456L)
 *     .addReply("789")
 *     .addAt("987654")
 *     .addText(" 你好！")
 *     .addImage("http://example.com/img.png")
 *     .build();
 * }</pre>
 *
 * @see Builder
 */
public final class GroupMessage {

	@JsonProperty("group_id")
	private final Long groupId;

	@JsonProperty("message")
	private final List<AbstractMessageSegment> messageChain;

	/**
	 * 私有构造函数，防止外部直接实例化。
	 * @param builder 用于构建此对象的建造者
	 */
	private GroupMessage(Builder builder) {
		this.groupId = builder.groupId;
		// 创建一个不可修改的列表副本，确保 GroupMessage 实例的不可变性
		this.messageChain = List.copyOf(builder.messageChain);
	}

	/**
	 * 获取此消息的目标群组ID。
	 * @return 群组ID
	 */
	public Long getGroupId() {
		return groupId;
	}

	/**
	 * 获取此消息的消息链。
	 * @return 一个由消息段组成的不可修改的列表
	 */
	public List<AbstractMessageSegment> getMessageChain() {
		return messageChain;
	}

	/**
	 * 获取一个新的 {@link Builder} 实例来开始构建 {@link GroupMessage}。
	 * @return 新的建造者实例
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * {@link GroupMessage} 的建造者类，用于以链式调用的方式构建复杂消息。
	 */
	public static final class Builder {

		private Long groupId;
		private final List<AbstractMessageSegment> messageChain = new ArrayList<>();

		private Builder() {
			// 私有构造函数，强制通过 GroupMessage.builder() 获取实例
		}

		/**
		 * 设置目标群组ID。
		 * @param groupId 目标群号
		 * @return 当前建造者实例，用于链式调用
		 */
		public Builder groupId(long groupId) {
			this.groupId = groupId;
			return this;
		}

		/**
		 * 向消息链中添加一个预先构建好的消息段。
		 * @param segment 消息段对象
		 * @return 当前建造者实例
		 */
		public Builder addSegment(AbstractMessageSegment segment) {
			this.messageChain.add(segment);
			return this;
		}

		/**
		 * 添加一段纯文本。
		 * @param text 文本内容
		 * @return 当前建造者实例
		 */
		public Builder addText(String text) {
			return addSegment(new TextSegment(new TextData(text)));
		}

		/**
		 * 添加一个QQ表情。
		 * @param id QQ表情的ID
		 * @return 当前建造者实例
		 */
		public Builder addFace(String id) {
			return addSegment(new FaceSegment(new FaceData(id)));
		}

		/**
		 * 添加一张图片。
		 * @param file 图片的文件名、绝对路径、网络URL或Base64编码
		 * @return 当前建造者实例
		 */
		public Builder addImage(String file) {
			return addSegment(new ImageSegment(new ImageData()));
		}

		/**
		 * 添加一张闪照。
		 * @param file 图片的文件名、绝对路径、网络URL或Base64编码
		 * @return 当前建造者实例
		 */
		public Builder addFlashImage(String file) {
			return addSegment(new ImageSegment(new ImageData()));
		}

		/**
		 * 添加一段语音。
		 * @param file 语音的文件名、绝对路径、网络URL或Base64编码
		 * @return 当前建造者实例
		 */
		public Builder addRecord(String file) {
			return addSegment(new RecordSegment(new RecordData(file, false, null, null, null, null)));
		}

		/**
		 * 添加一段变声语音。
		 * @param file 语音的文件名、绝对路径、网络URL或Base64编码
		 * @return 当前建造者实例
		 */
		public Builder addMagicRecord(String file) {
			return addSegment(new RecordSegment(new RecordData(file, true, null, null, null, null)));
		}

		/**
		 * 添加一个短视频。
		 * @param file 视频的文件名、绝对路径、网络URL或Base64编码
		 * @return 当前建造者实例
		 */
		public Builder addVideo(String file) {
			return addSegment(new VideoSegment(new VideoData(file, null, null, null, null)));
		}

		/**
		 * 添加一个 @某人 的提醒。
		 * @param qq 要@的QQ号
		 * @return 当前建造者实例
		 */
		public Builder addAt(String qq) {
			return addSegment(new AtSegment(new AtData(qq)));
		}

		/**
		 * 添加一个 @全体成员 的提醒。
		 * @return 当前建造者实例
		 */
		public Builder addAtAll() {
			return addAt("all");
		}

		/** 添加一个猜拳魔法表情。 */
		public Builder addRps() {
			return addSegment(new RpsSegment());
		}

		/** 添加一个掷骰子魔法表情。 */
		public Builder addDice() {
			return addSegment(new DiceSegment());
		}


		/** 添加一个窗口抖动（戳一戳）。 */
		public Builder addShake() {
			return addSegment(new ShakeSegment());
		}

		/** 添加一个戳一戳表情。 */
		public Builder addPoke(String type, String id) {
			return addSegment(new PokeSegment(new PokeData(type, id, null)));
		}

		/**
		 * 标记本消息为匿名消息。
		 * @param ignore 无法匿名时是否继续发送，默认为 false
		 * @return 当前建造者实例
		 */
		public Builder addAnonymous(Boolean ignore) {
			return addSegment(new AnonymousSegment(new AnonymousData()));
		}

		/** 添加一个链接分享。 */
		public Builder addShare(String url, String title, String content, String image) {
			return addSegment(new ShareSegment(new ShareData(url, title, content, image)));
		}

		/** 推荐一个好友。 */
		public Builder addContactFriend(String userId) {
			return addSegment(new ContactSegment(new ContactData("qq", userId)));
		}

		/** 推荐一个群。 */
		public Builder addContactGroup(String groupId) {
			return addSegment(new ContactSegment(new ContactData("group", groupId)));
		}

		/** 添加一个位置分享。 */
		public Builder addLocation(double lat, double lon, String title, String content) {
			return addSegment(new LocationSegment(new LocationData(String.valueOf(lat), String.valueOf(lon), title, content)));
		}

		/** 添加一个标准音乐分享（QQ音乐、网易云、虾米）。 */
		public Builder addMusic(String type, String id) {
			return addSegment(new MusicSegment(new MusicData(type, id, null, null, null, null, null)));
		}

		/** 添加一个自定义音乐分享。 */
		public Builder addCustomMusic(String url, String audio, String title, String content, String image) {
			return addSegment(new MusicSegment(new MusicData("custom", null, url, audio, title, content, image)));
		}

		/**
		 * 添加一个回复。
		 * @param messageId 要回复的消息ID
		 * @return 当前建造者实例
		 */
		public Builder addReply(String messageId) {
			return addSegment(new ReplySegment(new ReplyData(messageId)));
		}

		/**
		 * 添加一个合并转发节点（通过消息ID）。
		 * @param messageId 要转发的消息ID
		 * @return 当前建造者实例
		 */
		public Builder addForwardNode(String messageId) {
			return addSegment(new NodeSegment(new NodeData()));
		}

		/**
		 * 添加一个自定义的合并转发节点。
		 * @param userId 发送者QQ号
		 * @param nickname 发送者昵称
		 * @param contentBuilder 一个用于构建此节点内消息链的 lambda
		 * @return 当前建造者实例
		 */
		public Builder addCustomForwardNode(String userId, String nickname, Consumer<Builder> contentBuilder) {
			Builder nodeContentBuilder = new Builder();
			contentBuilder.accept(nodeContentBuilder);
			NodeData nodeData = new NodeData();
			return addSegment(new NodeSegment(nodeData));
		}

		/** 添加一段XML消息。 */
		public Builder addXml(String data) {
			return addSegment(new XmlSegment(new XmlData(data)));
		}

		/** 添加一段JSON消息。 */
		public Builder addJson(String data) {
			return addSegment(new JsonSegment(new JsonData()));
		}

		/**
		 * 完成构建并返回一个不可变的 {@link GroupMessage} 实例。
		 * @return 构建完成的群消息对象
		 * @throws IllegalStateException 如果群组ID未被设置
		 */
		public GroupMessage build() {
			if (groupId == null) {
				throw new IllegalStateException("Group ID cannot be null. Please call .groupId() before building.");
			}
			return new GroupMessage(this);
		}
	}
}