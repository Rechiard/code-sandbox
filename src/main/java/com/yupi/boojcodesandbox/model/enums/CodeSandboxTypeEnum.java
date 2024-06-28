package com.yupi.boojcodesandbox.model.enums;

import org.springframework.util.ObjectUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 代码沙箱类型
 */
public enum CodeSandboxTypeEnum {

    EXAMPLE("示例沙箱", "example"),
    REMOTE("远程沙箱", "remote"),
    THIRD_PARTY("第三方沙箱", "thirdParty");


    private final String text;

    private final String value;

    CodeSandboxTypeEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 获取值列表
     *
     * @return
     */
    public static List<String> getValues() {
        return Arrays.stream(values()).map(item -> item.value).collect(Collectors.toList());
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value
     * @return
     */
    public static CodeSandboxTypeEnum getEnumByValue(String value) {
        if (ObjectUtils.isEmpty(value)) {
            return null;
        }
        for (CodeSandboxTypeEnum anEnum : CodeSandboxTypeEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }

    public String getValue() {
        return value;
    }

    public String getText() {
        return text;
    }
}
