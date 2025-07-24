//package com.shuanglin.bot.langchain4j.rag.config;
//
//import dev.langchain4j.model.input.Prompt;
//import dev.langchain4j.model.input.PromptTemplate;
//import dev.langchain4j.spi.prompt.PromptTemplateFactory;
//
//import java.time.Clock;
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.time.LocalTime;
//import java.util.HashMap;
//import java.util.Map;
//
//import static dev.langchain4j.internal.ValidationUtils.ensureNotBlank;
//import static dev.langchain4j.internal.ValidationUtils.ensureNotNull;
//import static dev.langchain4j.spi.ServiceHelper.loadFactories;
//import static java.util.Collections.singletonMap;
//
//public class ModelPromptTemplate {
//	static final String CURRENT_DATE = "current_date";
//	static final String CURRENT_TIME = "current_time";
//	static final String CURRENT_DATE_TIME = "current_date_time";
//
//	private final String templateString;
//	private final Clock clock;
//
//	/**
//	 * Create a new PromptTemplate.
//	 *
//	 * <p>The {@code Clock} will be the system clock.</p>
//	 *
//	 * @param template the template string of the prompt.
//	 */
//	public ModelPromptTemplate(String template) {
//		this(template, Clock.systemDefaultZone());
//	}
//
//	/**
//	 * Create a new PromptTemplate.
//	 *
//	 * @param template the template string of the prompt.
//	 * @param clock    the clock to use for the special variables.
//	 */
//	ModelPromptTemplate(String template, Clock clock) {
//		this.templateString = ensureNotBlank(template, "template");
//		this.clock = ensureNotNull(clock, "clock");
//	}
//
//	private static PromptTemplateFactory factory() {
//		for (PromptTemplateFactory factory : loadFactories(PromptTemplateFactory.class)) {
//			return factory;
//		}
//	}
//
//	public static PromptTemplate from(String template) {
//		return new PromptTemplate(template);
//	}
//
//	public String template() {
//		return templateString;
//	}
//
//	public Prompt apply(Object value) {
//		return apply(singletonMap("it", value));
//	}
//
//	public Prompt apply(Map<String, Object> variables) {
//		ensureNotNull(variables, "variables");
//		return Prompt.from(template.render(injectDateTimeVariables(variables)));
//	}
//
//	private Map<String, Object> injectDateTimeVariables(Map<String, Object> variables) {
//		Map<String, Object> variablesCopy = new HashMap<>(variables);
//		variablesCopy.put(CURRENT_DATE, LocalDate.now(clock));
//		variablesCopy.put(CURRENT_TIME, LocalTime.now(clock));
//		variablesCopy.put(CURRENT_DATE_TIME, LocalDateTime.now(clock));
//		return variablesCopy;
//	}
//}
