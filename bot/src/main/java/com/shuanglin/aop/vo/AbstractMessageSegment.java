package com.shuanglin.aop.vo;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.shuanglin.aop.vo.Segment.*;
import dev.langchain4j.data.segment.TextSegment;

@JsonTypeInfo(
		use = JsonTypeInfo.Id.NAME,
		include = JsonTypeInfo.As.PROPERTY,
		property = "type"
)
@JsonSubTypes({
		@JsonSubTypes.Type(value = TextSegment.class, name = "text"),
		@JsonSubTypes.Type(value = FaceSegment.class, name = "face"),
		@JsonSubTypes.Type(value = ImageSegment.class, name = "image"),
		@JsonSubTypes.Type(value = RecordSegment.class, name = "record"),
		@JsonSubTypes.Type(value = VideoSegment.class, name = "video"),
		@JsonSubTypes.Type(value = AtSegment.class, name = "at"),
		@JsonSubTypes.Type(value = RpsSegment.class, name = "rps"),
		@JsonSubTypes.Type(value = DiceSegment.class, name = "dice"),
		@JsonSubTypes.Type(value = ShakeSegment.class, name = "shake"),
		@JsonSubTypes.Type(value = PokeSegment.class, name = "poke"),
		@JsonSubTypes.Type(value = AnonymousSegment.class, name = "anonymous"),
		@JsonSubTypes.Type(value = ShareSegment.class, name = "share"),
		@JsonSubTypes.Type(value = ContactSegment.class, name = "contact"),
		@JsonSubTypes.Type(value = LocationSegment.class, name = "location"),
		@JsonSubTypes.Type(value = MusicSegment.class, name = "music"),
		@JsonSubTypes.Type(value = ReplySegment.class, name = "reply"),
		@JsonSubTypes.Type(value = ForwardSegment.class, name = "forward"),
		@JsonSubTypes.Type(value = NodeSegment.class, name = "node"),
		@JsonSubTypes.Type(value = XmlSegment.class, name = "xml"),
		@JsonSubTypes.Type(value = JsonSegment.class, name = "json")
})
public abstract class AbstractMessageSegment {
	// 基类保持不变
}