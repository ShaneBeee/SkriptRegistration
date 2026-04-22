package com.github.shanebeee.skr;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.Cloner;
import ch.njol.skript.classes.Parser;
import ch.njol.skript.classes.Serializer;
import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.conditions.base.PropertyCondition.PropertyType;
import ch.njol.skript.expressions.base.EventValueExpression;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.DefaultExpression;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.Section;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.function.Functions;
import ch.njol.skript.registrations.Classes;
import org.bukkit.Keyed;
import org.bukkit.Registry;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.addon.AddonModule;
import org.skriptlang.skript.addon.SkriptAddon;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValue;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValueRegistry;
import org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfos;
import org.skriptlang.skript.common.function.DefaultFunction;
import org.skriptlang.skript.lang.converter.Converter;
import org.skriptlang.skript.lang.entry.EntryValidator;
import org.skriptlang.skript.lang.structure.Structure;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;
import org.skriptlang.skript.util.Priority;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Registration for registering elements to Skript.
 */
@SuppressWarnings({"rawtypes", "unchecked", "unused"})
public class Registration {

    private final SkriptAddon addon;
    private final RegistrationAddonModule module;
    private final List<Registrar<?>> preRegistrations = new ArrayList<>();
    private final List<TypeRegistrar<?>> types = new ArrayList<>();
    private final List<EffectRegistrar> effects = new ArrayList<>();
    private final List<ConditionRegistrar> conditions = new ArrayList<>();
    private final List<EventRegistrar> events = new ArrayList<>();
    private final List<SectionRegistrar> sections = new ArrayList<>();
    private final List<ExpressionRegistrar> expressions = new ArrayList<>();
    private final List<StructureRegistrar<?>> structures = new ArrayList<>();
    private final List<FunctionRegistrar> functions = new ArrayList<>();
    private final List<EventValueRegistrar<?, ?>> eventValues = new ArrayList<>();
    private final Map<Class<? extends Event>, List<EventValueRegistrar<?, ?>>> eventValuesByEvent = new HashMap<>();

    /**
     * Create a new registration.
     *
     * @param name        Name of the addon
     * @param includeLang Whether to include the language file
     */
    public Registration(String name, boolean includeLang) {
        this.addon = Skript.instance().registerAddon(RegistrationAddonModule.class, name);
        if (includeLang) {
            this.addon.localizer().setSourceDirectories("lang", null);
        }
        this.module = new RegistrationAddonModule(name, this);
    }

    /**
     * Get the {@link SkriptAddon addon} this registration belongs to.
     *
     * @return The addon this registration belongs to
     */
    public SkriptAddon getAddon() {
        return this.addon;
    }

    /**
     * Get all registered {@link ClassInfo ClassInfos}.
     *
     * @return All ClassInfos
     */
    public List<TypeRegistrar<?>> getTypes() {
        return this.types;
    }

    /**
     * Get all registered {@link Effect Effects}.
     *
     * @return All effects
     */
    public List<EffectRegistrar> getEffects() {
        return this.effects;
    }

    /**
     * Get all registered {@link Condition Conditions}.
     *
     * @return All conditions
     */
    public List<ConditionRegistrar> getConditions() {
        return this.conditions;
    }

    /**
     * Get all registered {@link Event Events}.
     *
     * @return All events
     */
    public List<EventRegistrar> getEvents() {
        return this.events;
    }

    /**
     * Get all registered {@link Section Sections}.
     *
     * @return All sections
     */
    public List<SectionRegistrar> getSections() {
        return this.sections;
    }

    /**
     * Get all registered {@link Expression Expressions}.
     *
     * @return All expressions
     */
    public List<ExpressionRegistrar> getExpressions() {
        return this.expressions;
    }

    /**
     * Get all registered {@link Structure Structures}.
     *
     * @return All structures
     */
    public List<StructureRegistrar<?>> getStructures() {
        return this.structures;
    }

    /**
     * Get all registered {@link ch.njol.skript.lang.function.Function Functions}.
     *
     * @return All functions
     */
    public List<FunctionRegistrar> getFunctions() {
        return this.functions;
    }

    /**
     * Get all registered {@link EventValue EventValues}.
     *
     * @return All event values
     */
    public List<EventValueRegistrar<?, ?>> getEventValues() {
        return this.eventValues;
    }

    /**
     * Get all {@link EventValue EventValues} that are registered for the given event class.
     *
     * @param eventClass Event class to get event values for.
     * @return List of event values.
     */
    public @NotNull List<EventValueRegistrar<?, ?>> getEventValues(Class<? extends Event> eventClass) {
        if (this.eventValuesByEvent.containsKey(eventClass)) {
            return this.eventValuesByEvent.get(eventClass);
        }

        for (Class<? extends Event> aClass : this.eventValuesByEvent.keySet()) {
            if (aClass.isAssignableFrom(eventClass)) {
                return this.eventValuesByEvent.get(aClass);
            }
        }
        return List.of();
    }

    /**
     * Base Registrar for all Skript element registrations.
     *
     * @param <T> Type of registrar
     */
    @SuppressWarnings("unchecked")
    public class Registrar<T extends Registrar<T>> {
        private final Documentation documentation = new Documentation();
        private boolean registered;

        private Registrar() {
            Registration.this.preRegistrations.add(this);
        }

        /**
         * Exclude docs for this syntax.
         *
         * @return This registrar for chaining.
         */
        public T noDoc() {
            this.documentation.setNoDoc(true);
            return (T) this;
        }

        /**
         * The documentation name of this syntax.
         *
         * @param name The name of this syntax.
         * @return This registrar for chaining.
         */
        public T name(String name) {
            this.documentation.setName(name);
            return (T) this;
        }

        /**
         * The documentation description of this syntax.
         *
         * @param description The description of this syntax.
         * @return This registrar for chaining.
         */
        public T description(String... description) {
            this.documentation.setDescription(description);
            return (T) this;
        }

        /**
         * The documentation examples of this syntax.
         *
         * @param examples The examples of this syntax.
         * @return This registrar for chaining.
         */
        public T examples(String... examples) {
            this.documentation.setExamples(examples);
            return (T) this;
        }

        /**
         * The documentation search keywords of this syntax.
         *
         * @param keywords The search keywords of this syntax.
         * @return This registrar for chaining.
         */
        public T keywords(String... keywords) {
            this.documentation.setKeywords(keywords);
            return (T) this;
        }

        /**
         * When this syntax was added.
         *
         * @param since When this syntax was added.
         * @return This registrar for chaining.
         */
        public T since(String... since) {
            this.documentation.setSince(since);
            return (T) this;
        }

        /**
         * Get the documentation of this syntax.
         *
         * @return The documentation of this syntax.
         */
        public Documentation getDocumentation() {
            return this.documentation;
        }

        /**
         * Check if this syntax is registered.
         *
         * @return Whether this syntax is registered.
         */
        public boolean isRegistered() {
            return this.registered;
        }

        /**
         * Finalize registration for this registrar.
         */
        public void register() {
            if (this.registered) {
                skriptError("Syntax '%s' is already registered!", this.documentation.getName());
                return;
            }
            this.registered = true;
        }
    }

    /**
     * Registrar for {@link ClassInfo ClassInfos}.
     *
     * @param <T> Type of class to register.
     */
    public class TypeRegistrar<T> extends Registrar<TypeRegistrar<T>> {
        final Class<T> type;
        final String codename;
        String[] user;
        String[] after;
        String[] before;
        String usage;
        DefaultExpression<T> defaultExpression;
        @Nullable Supplier<Iterator<T>> supplier;
        Parser<? extends T> parser;
        Serializer<? super T> serializer;
        Cloner<T> cloner;
        Changer<? super T> changer;

        private TypeRegistrar(Class<T> type, String codename) {
            this.type = type;
            this.codename = codename;
        }

        public TypeRegistrar<T> user(String... user) {
            this.user = user;
            return this;
        }

        public TypeRegistrar<T> after(String... after) {
            this.after = after;
            return this;
        }

        public TypeRegistrar<T> before(String... before) {
            this.before = before;
            return this;
        }

        public TypeRegistrar<T> usage(String usage) {
            this.usage = usage;
            return this;
        }

        public TypeRegistrar<T> defaultExpression(DefaultExpression<T> defaultExpression) {
            this.defaultExpression = defaultExpression;
            return this;
        }

        public TypeRegistrar<T> supplier(Supplier<Iterator<T>> supplier) {
            this.supplier = supplier;
            return this;
        }

        public TypeRegistrar<T> parser(Parser<? extends T> parser) {
            this.parser = parser;
            return this;
        }

        public TypeRegistrar<T> serializer(Serializer<? super T> serializer) {
            this.serializer = serializer;
            return this;
        }

        public TypeRegistrar<T> cloner(Cloner<T> cloner) {
            this.cloner = cloner;
            return this;
        }

        public TypeRegistrar<T> changer(Changer<? super T> changer) {
            this.changer = changer;
            return this;
        }

        public void register() {
            super.register();
            Registration.this.types.add(this);
        }
    }

    /**
     * Get a new {@link TypeRegistrar TypeRegistrar} for a custom {@link ClassInfo}.
     *
     * @param type     Class to register
     * @param codename Codename of new type
     * @param <T>      Type of class to register.
     * @return New Type Registrar (Don't forget to register it!)
     */
    public <T> TypeRegistrar<T> newType(Class<T> type, String codename) {
        return new TypeRegistrar<>(type, codename);
    }

    /**
     * Registrar for Enum {@link ClassInfo ClassInfos}.
     *
     * @param <T>
     */
    public class EnumTypeRegistrar<T extends Enum<T>> extends TypeRegistrar<T> {
        final String prefix;
        final String suffix;
        final @NotNull EnumWrapper<T> enumWrapper;
        final ClassInfo<T> classInfo;

        private EnumTypeRegistrar(Class<T> type, String codename, String prefix, String suffix, boolean plurals) {
            super(type, codename);
            this.prefix = prefix;
            this.suffix = suffix;
            this.enumWrapper = new EnumWrapper<>(type, prefix, suffix, plurals);
            this.usage = this.enumWrapper.getAllNames();
            this.classInfo = this.enumWrapper.getClassInfo(codename);
        }

        private EnumTypeRegistrar(Class<T> type, @NotNull EnumWrapper<T> enumWrapper, String codename, String prefix, String suffix) {
            super(type, codename);
            this.prefix = prefix;
            this.suffix = suffix;
            this.enumWrapper = enumWrapper;
            this.usage = this.enumWrapper.getAllNames();
            this.classInfo = this.enumWrapper.getClassInfo(codename);
        }
    }

    /**
     * Create a new {@link EnumTypeRegistrar} for a custom {@link EnumWrapper EnumClassInfo}.
     *
     * @param type     Class to register
     * @param codename Codename of new type
     * @param <T>      Type of class to register.
     * @return New EnumTypeRegistrar (Don't forget to register it!)
     */
    public <T extends Enum<T>> EnumTypeRegistrar<T> newEnumType(Class<T> type, String codename) {
        return new EnumTypeRegistrar<>(type, codename, null, null, false);
    }

    /**
     * Create a new {@link EnumTypeRegistrar} for a custom {@link EnumWrapper EnumClassInfo} with a custom prefix and suffix.
     *
     * @param type     Class to register
     * @param codename Codename of new type
     * @param plurals  Whether to include plural forms of the enum names
     * @param <T>      Type of class to register.
     * @return New EnumTypeRegistrar (Don't forget to register it!)
     */
    public <T extends Enum<T>> EnumTypeRegistrar<T> newEnumType(Class<T> type, String codename, boolean plurals) {
        return new EnumTypeRegistrar<>(type, codename, null, null, plurals);
    }

    /**
     * Create a new {@link EnumTypeRegistrar} for a custom {@link EnumWrapper EnumClassInfo} with a custom prefix and suffix.
     *
     * @param type     Class to register
     * @param codename Codename of new type
     * @param prefix   Custom prefix for enum names
     * @param suffix   Custom suffix for enum names
     * @param <T>      Type of class to register.
     * @return New EnumTypeRegistrar (Don't forget to register it!)
     */
    public <T extends Enum<T>> EnumTypeRegistrar<T> newEnumType(Class<T> type, String codename, String prefix, String suffix) {
        return new EnumTypeRegistrar<>(type, codename, prefix, suffix, false);
    }

    /**
     * Create a new @link EnumTypeRegistrar} for a custom {@link EnumWrapper EnumClassInfo} with a custom prefix and suffix.
     *
     * @param type     Class to register
     * @param codename Codename of new type
     * @param prefix   Custom prefix for enum names
     * @param suffix   Custom suffix for enum names
     * @param plurals  Whether to include plural forms of the enum names
     * @param <T>      Type of class to register.
     * @return New EnumTypeRegistrar (Don't forget to register it!)
     */
    public <T extends Enum<T>> EnumTypeRegistrar<T> newEnumType(Class<T> type, String codename, String prefix, String suffix, boolean plurals) {
        return new EnumTypeRegistrar<>(type, codename, prefix, suffix, plurals);
    }

    /**
     * Create a new {@link EnumTypeRegistrar} for a custom {@link EnumWrapper EnumClassInfo} with a custom prefix and suffix.
     *
     * @param type        Class to register
     * @param enumWrapper Custom wrapper for enum values
     * @param codename    Codename of new type
     * @param <T>         Type of class to register.
     * @return New EnumTypeRegistrar (Don't forget to register it!)
     */
    public <T extends Enum<T>> EnumTypeRegistrar<T> newEnumType(Class<T> type, EnumWrapper<T> enumWrapper, String codename) {
        return new EnumTypeRegistrar<>(type, enumWrapper, codename, null, null);
    }

    /**
     * Create a new {@link EnumTypeRegistrar} for a custom {@link EnumWrapper EnumClassInfo} with a custom prefix and suffix.
     *
     * @param type        Class to register
     * @param enumWrapper Custom wrapper for enum values
     * @param codename    Codename of new type
     * @param prefix      Custom prefix for enum names
     * @param suffix      Custom suffix for enum names
     * @param <T>         Type of class to register.
     * @return New EnumTypeRegistrar (Don't forget to register it!)
     */
    public <T extends Enum<T>> EnumTypeRegistrar<T> newEnumType(Class<T> type, EnumWrapper<T> enumWrapper, String codename, String prefix, String suffix) {
        return new EnumTypeRegistrar<>(type, enumWrapper, codename, prefix, suffix);
    }

    /**
     * Registrar for {@link Registry} {@link ClassInfo ClassInfos}.
     *
     * @param <T>
     */
    public class RegistryTypeRegistrar<T extends Keyed> extends TypeRegistrar<T> {
        final Registry<T> registry;
        final String prefix;
        final String suffix;
        final boolean createUsage;
        final ClassInfo<T> classInfo;

        private RegistryTypeRegistrar(Registry<T> registry, Class<T> type, String codename, boolean createUsage, String prefix, String suffix) {
            super(type, codename);
            this.registry = registry;
            this.prefix = prefix;
            this.suffix = suffix;
            this.createUsage = createUsage;

            this.classInfo = RegistryClassInfo.create(registry, type, createUsage, codename, prefix, suffix);
            @Nullable String[] classInfoUsage = this.classInfo.getUsage();
            if (classInfoUsage != null) {
                this.usage = String.join(", ", classInfoUsage);
            }
        }
    }

    /**
     * Create a new RegistryTypeRegistrar.
     *
     * @param registry Registry instance
     * @param type     Class to register
     * @param codename Codename of new type
     * @param <T>      Type of class to register.
     * @return New RegistryTypeRegistrar (Don't forget to register it!)
     */
    public <T extends Keyed> RegistryTypeRegistrar<T> newRegistryType(Registry<T> registry, Class<T> type, String codename) {
        return new RegistryTypeRegistrar<>(registry, type, codename, true, null, null);
    }

    /**
     * Create a new RegistryTypeRegistrar with a custom prefix and suffix.
     *
     * @param registry    Registry instance
     * @param type        Class to register
     * @param createUsage Whether to create usage for the registry type
     * @param codename    Codename of new type
     * @param <T>         Type of class to register.
     * @return New RegistryTypeRegistrar (Don't forget to register it!)
     */
    public <T extends Keyed> RegistryTypeRegistrar<T> newRegistryType(Registry<T> registry, Class<T> type, boolean createUsage, String codename) {
        return new RegistryTypeRegistrar<>(registry, type, codename, createUsage, null, null);
    }

    /**
     * Create a new RegistryTypeRegistrar with a custom prefix and suffix.
     *
     * @param registry Registry instance
     * @param type     Class to register
     * @param codename Codename of new type
     * @param prefix   Custom prefix for registry type names
     * @param suffix   Custom suffix for registry type names
     * @param <T>      Type of class to register.
     * @return New RegistryTypeRegistrar (Don't forget to register it!)
     */
    public <T extends Keyed> RegistryTypeRegistrar<T> newRegistryType(Registry<T> registry, Class<T> type, String codename, String prefix, String suffix) {
        return new RegistryTypeRegistrar<>(registry, type, codename, true, prefix, suffix);
    }

    /**
     * Registrar for {@link Effect Effects}.
     */
    public class EffectRegistrar extends Registrar<EffectRegistrar> {
        final Class<? extends Effect> effect;
        final String[] patterns;

        private EffectRegistrar(Class<? extends Effect> effect, String[] patterns) {
            this.effect = effect;
            this.patterns = patterns;
        }

        public void register() {
            super.register();
            Registration.this.effects.add(this);
        }
    }

    /**
     * Create a new EffectRegistrar for a custom effect with patterns.
     *
     * @param effect   Class of the effect to register
     * @param patterns Patterns for the effect
     * @return New EffectRegistrar (Don't forget to register it!)
     */
    public EffectRegistrar newEffect(Class<? extends Effect> effect, String... patterns) {
        return new EffectRegistrar(effect, patterns);
    }

    /**
     * Registrar for {@link Section Sections}.
     */
    public class SectionRegistrar extends Registrar<SectionRegistrar> {
        final Class<? extends Section> section;
        final String[] patterns;
        final @Nullable EntryValidator validator;

        private SectionRegistrar(Class<? extends Section> section, String[] patterns, @Nullable EntryValidator validator) {
            this.section = section;
            this.patterns = patterns;
            this.validator = validator;
        }

        public void register() {
            super.register();
            Registration.this.sections.add(this);
        }
    }

    /**
     * Create a new SectionRegistrar for a custom section with patterns.
     *
     * @param section  Class of the section to register
     * @param patterns Patterns for the section
     * @return New SectionRegistrar (Don't forget to register it!)
     */
    public SectionRegistrar newSection(Class<? extends Section> section, String... patterns) {
        return new SectionRegistrar(section, patterns, null);
    }

    /**
     * Create a new SectionRegistrar for a custom section with patterns and validator.
     *
     * @param section   Class of the section to register
     * @param validator Validator for the section entries
     * @param patterns  Patterns for the section
     * @return New SectionRegistrar (Don't forget to register it!)
     */
    public SectionRegistrar newSection(Class<? extends Section> section, EntryValidator validator, String... patterns) {
        return new SectionRegistrar(section, patterns, validator);
    }

    /**
     * Registrar for {@link Condition Conditions}.
     */
    public class ConditionRegistrar extends Registrar<ConditionRegistrar> {
        final Class<? extends Condition> condition;
        final String[] patterns;

        private ConditionRegistrar(Class<? extends Condition> condition, String[] patterns) {
            this.condition = condition;
            this.patterns = patterns;
        }

        public void register() {
            super.register();
            Registration.this.conditions.add(this);
        }
    }

    /**
     * Create a new ConditionRegistrar for a custom condition with patterns.
     *
     * @param condition Class of the condition to register
     * @param patterns  Patterns for the condition
     * @return New ConditionRegistrar (Don't forget to register it!)
     */
    public ConditionRegistrar newCondition(Class<? extends Condition> condition, String... patterns) {
        return new ConditionRegistrar(condition, patterns);
    }

    /**
     * Registrar for {@link PropertyCondition PropertyConditions}.
     */
    public class PropertyConditionRegistrar extends ConditionRegistrar {
        public PropertyConditionRegistrar(Class<? extends Condition> condition, PropertyType type, String property, String owner) {
            super(condition, PropertyCondition.getPatterns(type, property, owner));
        }
    }

    /**
     * Create a new PropertyConditionRegistrar for a custom condition with property and owner.
     *
     * @param condition Class of the condition to register
     * @param type      Type of property condition
     * @param property  Property name
     * @param owner     Owner of the property
     * @return New PropertyConditionRegistrar (Don't forget to register it!)
     */
    public PropertyConditionRegistrar newPropertyCondition(Class<? extends Condition> condition, PropertyType type, String property, String owner) {
        return new PropertyConditionRegistrar(condition, type, property, owner);
    }

    /**
     * Create a new PropertyConditionRegistrar for a custom condition with property and owner.
     *
     * @param condition Class of the condition to register
     * @param property  Property name
     * @param owner     Owner of the property
     * @return New PropertyConditionRegistrar (Don't forget to register it!)
     */
    public PropertyConditionRegistrar newPropertyCondition(Class<? extends Condition> condition, String property, String owner) {
        return new PropertyConditionRegistrar(condition, PropertyType.BE, property, owner);
    }

    /**
     * Registrar for {@link Event Events}.
     */
    public class EventRegistrar extends Registrar<EventRegistrar> {
        final Class<? extends SkriptEvent> skriptEventClass;
        final Class<? extends Event>[] eventClasses;
        final String[] patterns;

        private EventRegistrar(Class<? extends SkriptEvent> skriptEventClass, Class<? extends Event>[] eventClass, String[] patterns) {
            this.skriptEventClass = skriptEventClass;
            this.eventClasses = eventClass;
            this.patterns = patterns;
        }

        public void register() {
            super.register();
            Registration.this.events.add(this);
        }
    }

    /**
     * Create a new EventRegistrar for a custom event with patterns.
     *
     * @param skriptEventClass Class of the SkriptEvent to register
     * @param eventClass       Class of the Event to register
     * @param patterns         Patterns for the event
     * @return New EventRegistrar (Don't forget to register it!)
     */
    public EventRegistrar newEvent(Class<? extends SkriptEvent> skriptEventClass, Class<? extends Event> eventClass, String... patterns) {
        return new EventRegistrar(skriptEventClass, new Class[]{eventClass}, patterns);
    }

    /**
     * Create a new EventRegistrar for a custom event with multiple event classes and patterns.
     *
     * @param skriptEventClass Class of the SkriptEvent to register
     * @param eventClasses     Classes of the Events to register
     * @param patterns         Patterns for the event
     * @return New EventRegistrar (Don't forget to register it!)
     */
    public EventRegistrar newEvent(Class<? extends SkriptEvent> skriptEventClass, Class<? extends Event>[] eventClasses, String... patterns) {
        return new EventRegistrar(skriptEventClass, eventClasses, patterns);
    }

    /**
     * Registrar for {@link Expression Expressions}.
     *
     * @param <T> Return type of the expression.
     * @param <E> Class of the Expression to register.
     */
    public class ExpressionRegistrar<T, E extends Expression<T>> extends Registrar<ExpressionRegistrar<T, E>> {
        final Class<E> expressionClass;
        final Class<T> returnType;
        final Priority priority;
        final String[] patterns;

        private ExpressionRegistrar(Class<E> expressionClass, Class<T> returnType, Priority priority, String[] patterns) {
            this.expressionClass = expressionClass;
            this.returnType = returnType;
            this.priority = priority;
            this.patterns = patterns;
        }

        public void register() {
            super.register();
            Registration.this.expressions.add(this);
        }
    }

    /**
     * Create a new ExpressionRegistrar for a custom {@link Expression} with patterns.
     *
     * @param expressionClass Class of the expression to register
     * @param returnType      Return type of the expression
     * @param priority        Priority of the expression
     * @param patterns        Patterns to match for the expression
     * @param <T>             Return type of the expression
     * @param <E>             Type of the expression
     * @return ExpressionRegistrar for the custom expression
     */
    public <T, E extends Expression<T>> ExpressionRegistrar<T, E> newExpression(Class<E> expressionClass, Class<T> returnType, Priority priority, String... patterns) {
        return new ExpressionRegistrar(expressionClass, returnType, priority, patterns);
    }

    /**
     * Create a new ExpressionRegistrar for a custom {@link ch.njol.skript.lang.util.SimpleExpression} with patterns.
     *
     * @param expressionClass Class of the expression to register
     * @param returnType      Return type of the expression
     * @param patterns        Patterns to match for the expression
     * @param <T>             Return type of the expression
     * @param <E>             Type of the expression
     * @return ExpressionRegistrar for the custom expression
     */
    public <T, E extends Expression<T>> ExpressionRegistrar<T, E> newSimpleExpression(Class<E> expressionClass, Class<T> returnType, String... patterns) {
        return new ExpressionRegistrar(expressionClass, returnType, SyntaxInfo.SIMPLE, patterns);
    }

    /**
     * Create a new ExpressionRegistrar for a custom {@link EventValueExpression} with patterns.
     *
     * @param expressionClass Class of the expression to register
     * @param returnType      Return type of the expression
     * @param patterns        Patterns to match for the expression
     * @param <T>             Return type of the expression
     * @param <E>             Type of the expression
     * @return ExpressionRegistrar for the custom expression
     */
    public <T, E extends Expression<T>> ExpressionRegistrar<T, E> newEventExpression(Class<E> expressionClass, Class<T> returnType, String... patterns) {
        return new ExpressionRegistrar(expressionClass, returnType, EventValueExpression.DEFAULT_PRIORITY, patterns);
    }

    /**
     * Create a new ExpressionRegistrar for a custom {@link Expression} with patterns.
     *
     * @param expressionClass Class of the expression to register
     * @param returnType      Return type of the expression
     * @param patterns        Patterns to match for the expression
     * @param <T>             Return type of the expression
     * @param <E>             Type of the expression
     * @return ExpressionRegistrar for the custom expression
     */
    public <T, E extends Expression<T>> ExpressionRegistrar<T, E> newCombinedExpression(Class<E> expressionClass, Class<T> returnType, String... patterns) {
        return new ExpressionRegistrar(expressionClass, returnType, SyntaxInfo.COMBINED, patterns);
    }

    /**
     * Create a new ExpressionRegistrar for a custom {@link SimplePropertyExpression} with patterns.
     *
     * @param expressionClass Class of the expression to register
     * @param returnType      Return type of the expression
     * @param property        Property name for the expression
     * @param owner           Owner class for the expression
     * @param <T>             Return type of the expression
     * @param <E>             Type of the expression
     * @return ExpressionRegistrar for the custom expression
     */
    public <T, E extends Expression<T>> ExpressionRegistrar newPropertyExpression(Class<E> expressionClass, Class<T> returnType, String property, String owner) {
        return new ExpressionRegistrar(expressionClass, returnType, SyntaxInfo.SIMPLE,
            SimplePropertyExpression.getPatterns(property, owner));
    }

    /**
     * Registrar for {@link Structure Structures}.
     *
     * @param <E> Type of the structure
     */
    public class StructureRegistrar<E extends Structure> extends Registrar<StructureRegistrar<E>> {
        final Class<E> structureClass;
        final String[] patterns;
        final EntryValidator validator;

        private StructureRegistrar(Class<E> structureClass, EntryValidator validator, String[] patterns) {
            this.structureClass = structureClass;
            this.validator = validator;
            this.patterns = patterns;
        }

        public void register() {
            super.register();
            Registration.this.structures.add(this);
        }
    }

    /**
     * Create a new StructureRegistrar for a custom {@link Structure} with patterns.
     *
     * @param structureClass Class of the structure to register
     * @param patterns       Patterns to match for the structure
     * @return StructureRegistrar for the custom structure
     */
    public StructureRegistrar<?> newStructure(Class<? extends Structure> structureClass, String... patterns) {
        return new StructureRegistrar<>(structureClass, null, patterns);
    }

    /**
     * Create a new StructureRegistrar for a custom {@link Structure} with patterns and validator.
     *
     * @param structureClass Class of the structure to register
     * @param entryValidator Validator for structure entries
     * @param patterns       Patterns to match for the structure
     * @return StructureRegistrar for the custom structure
     */
    public StructureRegistrar<?> newStructure(Class<? extends Structure> structureClass, EntryValidator entryValidator, String... patterns) {
        return new StructureRegistrar<>(structureClass, entryValidator, patterns);
    }

    /**
     * Registar for {@link ch.njol.skript.lang.function.Function Functions}.
     *
     * @param <T>
     */
    public class FunctionRegistrar<T> extends Registrar<FunctionRegistrar<T>> {
        final DefaultFunction<T> function;

        private FunctionRegistrar(DefaultFunction<T> function) {
            this.function = function;
        }

        @Override
        public void register() {
            super.register();
            Registration.this.functions.add(this);
        }
    }

    /**
     * Create a new FunctionRegistrar for a custom {@link DefaultFunction Function}.
     *
     * @param function function to register
     * @param <T>      return type of the function
     * @return FunctionRegistrar for the custom function
     */
    public <T> FunctionRegistrar<T> newFunction(DefaultFunction<T> function) {
        return new FunctionRegistrar<T>(function);
    }

    /**
     * Registrar for {@link EventValue EventValues}.
     *
     * @param <E> Event class type
     * @param <T> Value class type
     */
    public class EventValueRegistrar<E extends Event, T> extends Registrar<EventValueRegistrar<E, T>> {
        final Class<E> eventClass;
        final Class<T> valueClass;
        Converter<E, T> converter;
        String[] patterns = null;
        final Map<ChangeMode, EventValue.Changer<E, T>> changerMap = new HashMap<>();
        EventValue.Time time = EventValue.Time.NOW;
        Class<E>[] excludedEvents = null;
        String excludeErrorMessage = null;

        private EventValueRegistrar(Class<E> eventClass, Class<T> valueClass) {
            this.eventClass = eventClass;
            this.valueClass = valueClass;
        }

        public EventValueRegistrar<E, T> converter(Converter<E, T> converter) {
            this.converter = converter;
            return this;
        }

        public EventValueRegistrar<E, T> patterns(String... patterns) {
            this.patterns = patterns;
            return this;
        }

        public EventValueRegistrar<E, T> changer(ChangeMode mode, EventValue.Changer<E, T> changer) {
            this.changerMap.put(mode, changer);
            return this;
        }

        public EventValueRegistrar<E, T> time(int time) {
            this.time = EventValue.Time.of(time);
            return this;
        }

        public EventValueRegistrar<E, T> time(EventValue.Time time) {
            this.time = time;
            return this;
        }

        @SafeVarargs
        public final EventValueRegistrar<E, T> excludes(String excludeErrorMessage, Class<E>... excludedEvents) {
            this.excludeErrorMessage = excludeErrorMessage;
            this.excludedEvents = excludedEvents;
            return this;
        }

        @Override
        public void register() {
            super.register();
            Registration.this.eventValuesByEvent.computeIfAbsent(this.eventClass, k -> new ArrayList<>())
                .add(this);
            Registration.this.eventValues.add(this);
        }
    }

    /**
     * Create a new EventValueRegistrar for a custom {@link EventValue} with event and value classes.
     *
     * @param event Class of the event to register
     * @param value Class of the value to register
     * @param <F>   Type of the event
     * @param <T>   Type of the value
     * @return EventValueRegistrar for the custom event value
     */
    public <F extends Event, T> EventValueRegistrar<F, T> newEventValue(Class<F> event, Class<T> value) {
        return new EventValueRegistrar<>(event, value);
    }

    /**
     * Finalize the registration process.
     */
    public void finalizeRegistration() {
        this.addon.loadModules(this.module);
    }

    private void registerInit() {
        // CHECK REGISTRATION
        this.preRegistrations.forEach(registrar -> {
            if (!registrar.isRegistered()) {
                String registrarName = registrar.getClass().getSimpleName();
                String name = registrar.documentation.getName();
                if (name == null) {
                    // Try to find a name
                    if (registrar instanceof EventValueRegistrar<?, ?> evr) {
                        name = evr.eventClass.getSimpleName() + " " + evr.valueClass.getSimpleName();
                    } else {
                        name = registrarName;
                    }
                }
                if (name == null) {
                    skriptError("Unnamed registrar in '%s' not registered!", name);
                } else {
                    skriptError("Registrar for '%s' in '%s' not registered!", name, registrarName);
                }
            }
        });
        this.preRegistrations.clear();

        // TYPES
        for (TypeRegistrar type : getTypes()) {
            ClassInfo<?> classInfo;
            if (type instanceof EnumTypeRegistrar<?> enumTypeRegistrar) {
                classInfo = enumTypeRegistrar.classInfo;
            } else if (type instanceof RegistryTypeRegistrar<? extends Keyed> registryTypeRegistrar) {
                classInfo = registryTypeRegistrar.classInfo;
            } else {
                classInfo = new ClassInfo<>(type.type, type.codename);
            }
            if (type.user != null) {
                classInfo.user(type.user);
            }
            if (type.usage != null && classInfo.getUsage() == null) {
                classInfo.usage(type.usage);
            }
            if (type.before != null) {
                classInfo.before(type.before);
            }
            if (type.after != null) {
                classInfo.after(type.after);
            }
            if (type.defaultExpression != null) {
                classInfo.defaultExpression(type.defaultExpression);
            }
            if (type.supplier != null) {
                classInfo.supplier(type.supplier);
            }
            if (type.parser != null) {
                classInfo.parser(type.parser);
            }
            if (type.serializer != null) {
                classInfo.serializer(type.serializer);
            }
            if (type.cloner != null) {
                classInfo.cloner(type.cloner);
            }
            if (type.changer != null) {
                classInfo.changer(type.changer);
            }
            Classes.registerClass(classInfo);
        }
    }

    private void registerLoad() {
        SyntaxRegistry syntaxInfos = this.addon.syntaxRegistry();

        // STRUCTURES
        for (Registration.StructureRegistrar<?> structure : getStructures()) {

            DefaultSyntaxInfos.Structure<Structure> build = DefaultSyntaxInfos.Structure.builder(
                    (Class<Structure>) structure.structureClass)
                .addPatterns(structure.patterns)
                .entryValidator(structure.validator)
                .build();

            syntaxInfos.register(SyntaxRegistry.STRUCTURE, build);
        }
        // EVENTS
        for (Registration.EventRegistrar event : getEvents()) {
            BukkitSyntaxInfos.Event.Builder<? extends BukkitSyntaxInfos.Event.Builder<?, SkriptEvent>, SkriptEvent> builder = BukkitSyntaxInfos.Event.builder(
                (Class<SkriptEvent>) event.skriptEventClass,
                event.getDocumentation().getName());
            for (Class<? extends Event> eventClass : event.eventClasses) {
                builder.addEvent(eventClass);
            }
            builder.addPatterns(event.patterns);

            syntaxInfos.register(BukkitSyntaxInfos.Event.KEY, builder.build());
        }

        // EVENT VALUES
        EventValueRegistry eventValueRegistry = this.addon.registry(EventValueRegistry.class);
        for (EventValueRegistrar<?, ?> eventValue : getEventValues()) {
            Class eventClass = eventValue.eventClass;
            Class valueClass = eventValue.valueClass;
            EventValue.Time time = eventValue.time;
            boolean hasPatterns = eventValue.patterns != null;

            if (!hasPatterns && eventValueRegistry.isRegistered(eventClass, valueClass, time)) {
                debug("An event value has already been registered for %s / %s [%s]",
                    eventClass.getSimpleName(), valueClass.getSimpleName(), time);
                continue;
            }

            EventValue.Builder builder = EventValue.builder(eventClass, valueClass);
            builder.time(time);
            builder.getter(eventValue.converter);
            if (hasPatterns) {
                builder.patterns(eventValue.patterns);
            }
            eventValue.changerMap.forEach(builder::registerChanger);
            if (eventValue.excludedEvents != null) {
                builder.excludes(eventValue.excludedEvents);
                if (eventValue.excludeErrorMessage != null) {
                    builder.excludedErrorMessage(eventValue.excludeErrorMessage);
                }
            }
            eventValueRegistry.register(builder.build());
        }

        // SECTIONS
        for (Registration.SectionRegistrar section : getSections()) {

            Supplier<Section> supplier = () -> {
                try {
                    return section.section.getConstructor().newInstance();
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                         NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
            };

            syntaxInfos.register(SyntaxRegistry.SECTION, SyntaxInfo.builder((Class<Section>) section.section)
                .supplier(supplier)
                .addPatterns(section.patterns)
                .build());
        }

        // EFFECTS
        for (EffectRegistrar effect : getEffects()) {
            Supplier<Effect> supplier = () -> {
                try {
                    return effect.effect.getConstructor().newInstance();
                } catch (InvocationTargetException | InstantiationException | IllegalAccessException |
                         NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
            };
            syntaxInfos.register(SyntaxRegistry.EFFECT,
                SyntaxInfo.builder((Class<Effect>) effect.effect)
                    .supplier(supplier)
                    .addPatterns(effect.patterns)
                    .build()
            );
        }

        // EXPRESSIONS
        for (Registration.ExpressionRegistrar<?, ?> expression : getExpressions()) {
            Supplier<Expression> supplier = () -> {
                try {
                    return expression.expressionClass.getConstructor().newInstance();
                } catch (InvocationTargetException | InstantiationException | IllegalAccessException |
                         NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
            };

            SyntaxInfo info = SyntaxInfo.Expression.builder((Class<? extends Expression>) expression.expressionClass, expression.returnType)
                .supplier(supplier)
                .addPatterns(expression.patterns).build();

            syntaxInfos.register(SyntaxRegistry.EXPRESSION, (SyntaxInfo.Expression<?, ?>) info);
        }

        // FUNCTIONS
        for (FunctionRegistrar function : getFunctions()) {

            Functions.register(function.function);
        }

        // CONDITIONS
        for (ConditionRegistrar condition : getConditions()) {
            Supplier<Condition> supplier = () -> {
                try {
                    return condition.condition.getConstructor().newInstance();
                } catch (InvocationTargetException | InstantiationException | IllegalAccessException |
                         NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
            };
            syntaxInfos.register(SyntaxRegistry.CONDITION,
                SyntaxInfo.builder((Class<Condition>) condition.condition)
                    .supplier(supplier)
                    .addPatterns(condition.patterns)
                    .build()
            );
        }
    }

    private static void skriptError(String format, Object... args) {
        Utils.skriptError(format, args);
    }

    @SuppressWarnings("SameParameterValue")
    private static void debug(String format, Object... args) {
        Utils.debug(format, args);
    }

    /**
     * @hidden
     */
    public static class RegistrationAddonModule implements AddonModule {

        private final String name;
        private final Registration registration;

        private RegistrationAddonModule(String name, Registration registration) {
            this.name = name;
            this.registration = registration;
        }

        @Override
        public void init(SkriptAddon addon) {
            this.registration.registerInit();
        }

        @Override
        public void load(SkriptAddon addon) {
            this.registration.registerLoad();
        }

        @Override
        public String name() {
            return this.name;
        }

    }

}
