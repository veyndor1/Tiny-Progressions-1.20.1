package com.kashdeya.tinyprogressions;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.common.crafting.conditions.IConditionSerializer;

/** A small boolean grammar for the original feature switches, without evaluating code. */
public record FeatureCondition(String expression) implements ICondition {
    private static final ResourceLocation ID = ResourceLocation.tryBuild("tp", "feature");
    @Override public ResourceLocation getID() { return ID; }
    @Override public boolean test(IContext context) { return evaluate(expression); }
    public static boolean evaluate(String expression) {
        var parser = new Parser(expression);
        boolean result = parser.or();
        parser.space();
        if (parser.position != expression.length()) throw new IllegalArgumentException("Unsupported feature expression " + expression);
        return result;
    }
    private static final class Parser {
        private final String input;
        private int position;
        private Parser(String input) { this.input=input; }
        private void space() { while(position<input.length() && Character.isWhitespace(input.charAt(position))) position++; }
        private boolean take(String token) {
            space(); if(!input.startsWith(token,position)) return false;
            position+=token.length(); return true;
        }
        private boolean or() { boolean result=and(); while(take("||")) { boolean right=and(); result |= right; } return result; }
        private boolean and() { boolean result=equality(); while(take("&&")) { boolean right=equality(); result &= right; } return result; }
        private boolean equality() {
            boolean result=unary();
            while(true) {
                if(take("==")) result = result == unary();
                else if(take("!=")) result = result != unary();
                else return result;
            }
        }
        private boolean unary() {
            if(take("!")) return !unary();
            if(take("(")) { boolean result=or(); if(!take(")")) throw new IllegalArgumentException("Unclosed feature condition "+input); return result; }
            space(); int start=position;
            while(position<input.length() && (Character.isJavaIdentifierPart(input.charAt(position)) || input.charAt(position)=='.')) position++;
            String value=input.substring(start,position);
            if(value.equals("true")) return true;
            if(value.equals("false")) return false;
            if(!value.matches("(?:ConfigHandler|ArmorHandler|DamageHandler|FuelHandler)\\.\\w+")) throw new IllegalArgumentException("Unsupported feature expression "+input);
            return LegacyConfig.enabled(value);
        }
    }
    public static final class Serializer implements IConditionSerializer<FeatureCondition> {
        @Override public void write(JsonObject json, FeatureCondition condition) { json.addProperty("expression", condition.expression()); }
        @Override public FeatureCondition read(JsonObject json) { return new FeatureCondition(json.get("expression").getAsString()); }
        @Override public ResourceLocation getID() { return ID; }
    }
}
