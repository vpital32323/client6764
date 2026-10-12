package com.nova.setting;

public class BooleanSetting extends Setting {
    public boolean value;
    public BooleanSetting(String name, boolean value) { super(name); this.value = value; }
    public void toggle() { value = !value; }
}
