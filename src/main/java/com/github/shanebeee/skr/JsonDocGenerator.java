package com.github.shanebeee.skr;

import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.localization.Noun;
import ch.njol.skript.registrations.Classes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValue;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValueRegistry;
import org.skriptlang.skript.common.function.DefaultFunction;
import org.skriptlang.skript.common.function.Parameter;
import org.skriptlang.skript.lang.entry.EntryData;
import org.skriptlang.skript.lang.entry.EntryValidator;
import org.skriptlang.skript.lang.entry.SectionEntryData;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Generator for JSON documentation for Skript addons.
 */
@SuppressWarnings("unused")
public class JsonDocGenerator {

    private final Plugin plugin;
    private final String addonName;
    private final Registration registration;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private int total = 0;
    private final List<String> IDS = new ArrayList<>();

    public JsonDocGenerator(Plugin plugin, Registration registration) {
        this.plugin = plugin;
        this.addonName = plugin.getPluginMeta().getName().toLowerCase().replace(" ", "_");
        this.registration = registration;
    }

    /**
     * Initializes the generator.
     * <p>
     * Docs will be put into the plugin's data folder.
     * </p>
     */
    public void generateDocs() {
        long start = System.currentTimeMillis();
        Utils.log("Generating docs...");
        JsonObject mainDoc = new JsonObject();
        addMeta(mainDoc);

        generateTypes(mainDoc);
        generateStructures(mainDoc);
        generateEvents(mainDoc);
        generateSections(mainDoc);
        generateEffects(mainDoc);
        generateExpressions(mainDoc);
        generateConditions(mainDoc);
        generateFunctions(mainDoc);

        // Print to file
        printToFile(mainDoc);
        long fin = System.currentTimeMillis() - start;
        Utils.log("Finished generating %s docs in %sms", this.total, fin);
    }

    private void generateTypes(JsonObject mainDoc) {
        Utils.log("Generating types...");
        JsonArray typesArray = new JsonArray();

        for (Registration.TypeRegistrar<?> type : this.registration.getTypes()) {
            JsonObject syntaxObject = new JsonObject();

            Documentation documentation = type.getDocumentation();
            if (documentation.isNoDoc()) continue;

            if (documentation.getName() == null) {
                Utils.log("<red>Missing name for Type '%s'", type.type.getSimpleName());
                continue;
            }

            // Generic
            List<String> patterns = new ArrayList<>();
            for (String s : type.user) {
                String usage = s
                    .replaceAll("\\((.*?)\\)\\?", "[$1]")
                    .replaceAll("(.)\\?", "[$1]");
                patterns.add(usage);
            }
            generateGeneric("type", documentation, syntaxObject, patterns.toArray(new String[0]));

            // Usage
            if (type.usage != null) {
                syntaxObject.addProperty("usage", type.usage);
            }

            typesArray.add(syntaxObject);
        }
        this.total += typesArray.size();
        Utils.log("Generated %s types", typesArray.size());
        mainDoc.add("types", typesArray);
    }

    private void generateStructures(JsonObject mainDoc) {
        Utils.log("Generating structures...");
        JsonArray structuresArray = new JsonArray();

        for (Registration.StructureRegistrar<?> structure : this.registration.getStructures()) {
            JsonObject syntaxObject = new JsonObject();
            Documentation documentation = structure.getDocumentation();
            if (documentation.isNoDoc()) continue;

            if (documentation.getName() == null) {
                Utils.log("<red>Missing name for Structure '%s'", structure.structureClass.getSimpleName());
                continue;
            }

            // Generic
            generateGeneric("structure", documentation, syntaxObject, structure.patterns);

            // Entries
            generateSectionEntries(structure.validator, syntaxObject);

            structuresArray.add(syntaxObject);
        }

        this.total += structuresArray.size();
        Utils.log("Generated %s structures", structuresArray.size());
        mainDoc.add("structures", structuresArray);
    }

    private void generateEvents(JsonObject mainDoc) {
        Utils.log("Generating events...");
        JsonArray eventsArray = new JsonArray();

        for (Registration.EventRegistrar event : this.registration.getEvents()) {
            JsonObject syntaxObject = new JsonObject();

            Documentation documentation = event.getDocumentation();
            if (documentation.isNoDoc()) continue;

            if (documentation.getName() == null) {
                Utils.log("<red>Missing name for Type '%s'", event.skriptEventClass.getSimpleName());
                continue;
            }

            // Generic
            List<String> patterns = new ArrayList<>();
            for (String pattern : event.patterns) {
                patterns.add("[on] " + pattern);
            }
            generateGeneric("event", documentation, syntaxObject, patterns.toArray(new String[0]));

            // Cancelable
            boolean cancellable = false;
            for (Class<? extends Event> eventClass : event.eventClasses) {
                if (Cancellable.class.isAssignableFrom(eventClass)) {
                    cancellable = true;
                    break;
                }
            }
            syntaxObject.addProperty("cancellable", cancellable);

            // EventValues
            List<String> eventValueList = new ArrayList<>();
            JsonArray description = syntaxObject.getAsJsonArray("description");
            description.add(" ");
            description.add("**Event Values:**");

            for (Class<? extends Event> eventClass : event.eventClasses) {
                for (Registration.EventValueRegistrar<?, ?> eventValueInfo : this.registration.getEventValues(eventClass)) {
                    addEventValue(eventValueList, eventValueInfo, description);
                }
                EventValueRegistry eventValueRegistry = this.registration.getAddon().registry(EventValueRegistry.class);
                for (EventValue<?, ?> element : eventValueRegistry.elements()) {
                    if (!element.eventClass().isAssignableFrom(eventClass)) continue;

                    Registration.EventValueRegistrar<?, ?> info = this.registration.eventValueFromSkriptEventValue(element);
                    addEventValue(eventValueList, info, description);
                }
            }
            if (!eventValueList.isEmpty()) {
                // Only add description if there are event values
                syntaxObject.add("description", description);
                eventValueList.sort(String::compareTo);
                JsonArray eventValuesArray = new JsonArray();
                eventValueList.forEach(eventValuesArray::add);
                syntaxObject.add("event values", eventValuesArray);
            }

            eventsArray.add(syntaxObject);
        }

        this.total += eventsArray.size();
        Utils.log("Generated %s events", eventsArray.size());
        mainDoc.add("events", eventsArray);
    }

    private void addEventValue(List<String> eventValueList, Registration.EventValueRegistrar<?, ?> eventValueInfo, JsonArray description) {
        Class<?> valueClass = eventValueInfo.valueClass;
        boolean isArray = valueClass.isArray();
        if (isArray) {
            valueClass = valueClass.getComponentType();
        }
        ClassInfo<?> valueClassInfo = Classes.getSuperClassInfo(valueClass);
        if (valueClassInfo == null) return;

        Noun classInfoName = valueClassInfo.getName();
        String infoName = eventValueInfo.patterns != null && eventValueInfo.patterns.length > 0 ? eventValueInfo.patterns[0] : isArray ? classInfoName.getPlural() : classInfoName.getSingular();
        EventValue.Time time = eventValueInfo.time;
        String timeString = switch (time) {
            case NOW -> "event-";
            case FUTURE -> "future event-";
            case PAST -> "past event-";
        };

        String eventValueString = timeString + infoName;
        if (eventValueList.contains(eventValueString)) {
            return;
        }
        eventValueList.add(eventValueString);
        description.add(" - `" + eventValueString + "`");
        if (eventValueInfo.getDocumentation().getDescription() != null) {
            description.add("   - **Description**: " + String.join(" ", eventValueInfo.getDocumentation().getDescription()));
        }
        if (eventValueInfo.patterns != null && eventValueInfo.patterns.length > 1) {
            description.add("   - **Patterns**: `" + timeString + String.join("`, `" + timeString, eventValueInfo.patterns) + "`");
        }
        Map<ChangeMode, ? extends EventValue.Changer<?, ?>> changerMap = eventValueInfo.changerMap;
        if (!changerMap.isEmpty()) {
            description.add("   - **Changers**: " + String.join(", ", changerMap.keySet().stream().map(Enum::name).toList()));
        }
    }

    private void generateSections(JsonObject mainDoc) {
        Utils.log("Generating sections...");
        JsonArray sectionsArray = new JsonArray();

        for (Registration.SectionRegistrar section : this.registration.getSections()) {
            JsonObject syntaxObject = new JsonObject();

            Documentation documentation = section.getDocumentation();
            if (documentation.isNoDoc()) continue;

            if (documentation.getName() == null) {
                Utils.log("<red>Missing name for Section '%s'", section.section.getSimpleName());
                continue;
            }

            // Generic
            generateGeneric("section", documentation, syntaxObject, section.patterns);

            // Entries
            generateSectionEntries(section.validator, syntaxObject);

            sectionsArray.add(syntaxObject);
        }

        this.total += sectionsArray.size();
        Utils.log("Generated %s sections", sectionsArray.size());
        mainDoc.add("sections", sectionsArray);
    }

    private void generateEffects(JsonObject mainDoc) {
        Utils.log("Generating effects...");
        JsonArray effectsArray = new JsonArray();

        for (Registration.EffectRegistrar effect : this.registration.getEffects()) {
            JsonObject syntaxObject = new JsonObject();

            Documentation documentation = effect.getDocumentation();
            if (documentation.isNoDoc()) continue;

            if (documentation.getName() == null) {
                Utils.log("<red>Missing name for Effect '%s'", effect.effect.getSimpleName());
                continue;
            }

            // Generic
            generateGeneric("effect", documentation, syntaxObject, effect.patterns);

            // Entries (If it's a section effect)
            generateSectionEntries(effect.validator, syntaxObject);

            effectsArray.add(syntaxObject);
        }

        this.total += effectsArray.size();
        Utils.log("Generated %s effects", effectsArray.size());
        mainDoc.add("effects", effectsArray);
    }

    private void generateExpressions(JsonObject mainDoc) {
        Utils.log("Generating expressions...");
        JsonArray expressionsArray = new JsonArray();

        Utils.log("<#E40CF0>Checking changers, ignore incoming errors from Expression classes");
        for (Registration.ExpressionRegistrar<?, ?> expression : this.registration.getExpressions()) {
            JsonObject syntaxObject = new JsonObject();

            Documentation documentation = expression.getDocumentation();
            if (documentation.isNoDoc()) continue;

            if (documentation.getName() == null) {
                Utils.log("<red>Missing name for Expression '%s'", expression.expressionClass.getSimpleName());
                continue;
            }

            // Generic
            generateGeneric("expression", documentation, syntaxObject, expression.patterns);

            // Entries (If it's a section expression)
            generateSectionEntries(expression.validator, syntaxObject);

            // Return Type
            ClassInfo<?> returnInfo = Classes.getExactClassInfo(expression.returnType);
            if (returnInfo != null) {
                syntaxObject.addProperty("return type", returnInfo.getDocName());
            }

            // Changers
            JsonArray changerArray = new JsonArray();
            try {
                Expression<?> o = expression.expressionClass.getConstructor().newInstance();
                for (ChangeMode value : o.getAcceptedChangeModes().keySet()
                    .stream().sorted(Comparator.comparing(Enum::name)).toList()) {
                    changerArray.add(value.name().toLowerCase(Locale.ROOT));
                }
                if (!changerArray.isEmpty()) {
                    syntaxObject.add("changers", changerArray);
                }
            } catch (Exception ignore) {
            }

            expressionsArray.add(syntaxObject);
        }
        Utils.log("<#E40CF0>Finished checking changers, resume watching console");
        Utils.error("Test error");

        this.total += expressionsArray.size();
        Utils.log("Generated %s expressions.", expressionsArray.size());
        mainDoc.add("expressions", expressionsArray);
    }

    private void generateConditions(JsonObject mainDoc) {
        Utils.log("Generating conditions...");
        JsonArray conditonsArray = new JsonArray();

        for (Registration.ConditionRegistrar condition : this.registration.getConditions()) {
            JsonObject syntaxObject = new JsonObject();

            Documentation documentation = condition.getDocumentation();
            if (documentation.isNoDoc()) continue;

            if (documentation.getName() == null) {
                Utils.log("<red>Missing name for Condition '%s'", condition.condition.getSimpleName());
                continue;
            }

            // Generic
            generateGeneric("condition", documentation, syntaxObject, condition.patterns);

            conditonsArray.add(syntaxObject);
        }

        this.total += conditonsArray.size();
        Utils.log("Generated %s conditions", conditonsArray.size());
        mainDoc.add("conditions", conditonsArray);
    }

    @SuppressWarnings("UnstableApiUsage")
    private void generateFunctions(JsonObject mainDoc) {
        Utils.log("Generating functions...");
        JsonArray functionsArray = new JsonArray();

        for (Registration.FunctionRegistrar<?> function : this.registration.getFunctions()) {
            JsonObject syntaxObject = new JsonObject();

            Documentation documentation = function.getDocumentation();
            if (documentation.isNoDoc()) continue;

            if (documentation.getName() == null) {
                documentation.setName(function.function.name());
            }

            // Generic
            String pattern = generateFunctionPattern(function.function);
            generateGeneric("function", documentation, syntaxObject, new String[]{pattern}, false);

            // Return Type
            ClassInfo<?> returnInfo = Classes.getExactClassInfo(function.function.signature().returnType());
            if (returnInfo != null) {
                syntaxObject.addProperty("return type", returnInfo.getDocName());
            }

            functionsArray.add(syntaxObject);
        }

        this.total += functionsArray.size();
        Utils.log("Generated %s functions", functionsArray.size());
        mainDoc.add("functions", functionsArray);
    }

    private void addMeta(JsonObject mainDoc) {
        JsonObject meta = new JsonObject();
        meta.addProperty("version", this.plugin.getPluginMeta().getVersion());
        mainDoc.add("metadata", meta);
    }

    private void generateSectionEntries(EntryValidator validator, JsonObject syntaxObject) {
        if (validator == null) return;
        JsonArray entriesArray = new JsonArray();
        for (EntryData<?> entryDatum : validator.getEntryData()) {
            JsonObject entryObject = new JsonObject();
            entryObject.addProperty("name", entryDatum.getKey());
            entryObject.addProperty("isRequired", !entryDatum.isOptional());
            entryObject.addProperty("isSection", SectionEntryData.class.isAssignableFrom(entryDatum.getClass()));
            entriesArray.add(entryObject);
        }
        syntaxObject.add("entries", entriesArray);
    }

    private void generateGeneric(String type, Documentation documentation,
                                 JsonObject syntaxObject, @Nullable String[] patterns) {
        generateGeneric(type, documentation, syntaxObject, patterns, true);
    }

    private void generateGeneric(String type, Documentation documentation, JsonObject syntaxObject,
                                 @Nullable String[] patterns, boolean removeParseMarks) {
        // Generate ID
        String id;
        if (documentation.getId() != null) {
            id = documentation.getId();
        } else {
            id = generateId(type, documentation.getName());
        }
        syntaxObject.addProperty("id", id);

        // Name
        String name = documentation.getName();
        syntaxObject.addProperty("name", name);

        // Description
        String[] description = documentation.getDescription();
        if (description != null) {
            JsonArray descriptionArray = new JsonArray();
            for (String line : description) {
                descriptionArray.add(line);
            }
            syntaxObject.add("description", descriptionArray);
        }

        // Examples
        String[] examples = documentation.getExamples();
        if (examples != null) {
            JsonArray examplesArray = new JsonArray();
            for (String example : examples) {
                examplesArray.add(example);
            }
            syntaxObject.add("examples", examplesArray);
        }

        // Since
        String[] since = documentation.getSince();
        if (since != null) {
            JsonArray sinceArray = new JsonArray();
            for (String s : since) {
                sinceArray.add(s);
            }
            syntaxObject.add("since", sinceArray);
        } else {
            Utils.log("<red>Missing 'since' for '%s'", name);
        }

        if (patterns != null) {
            JsonArray patternArray = new JsonArray();
            for (String pattern : parsePatterns(patterns, removeParseMarks)) {
                patternArray.add(pattern);
            }
            syntaxObject.add("patterns", patternArray);
        }
    }

    private String generateId(String type, String name) {
        type = type.toLowerCase(Locale.ROOT).replace(" ", "_");
        name = name.toLowerCase(Locale.ROOT).replace(" ", "_");
        String id = String.format("%s:%s:%s", this.addonName, type, name);
        if (this.IDS.contains(id)) {
            Utils.log("<red>ID '%s' already exists", id);
        }
        this.IDS.add(id);
        return id;
    }

    @SuppressWarnings("UnstableApiUsage")
    private String generateFunctionPattern(DefaultFunction<?> function) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(function.name());
        stringBuilder.append("(");

        Parameter<?>[] all = function.signature().parameters().all();
        for (int i = 0; i < all.length; i++) {
            stringBuilder.append(all[i].toFormattedString());
            if (i < all.length - 1) {
                stringBuilder.append(", ");
            }
        }

        stringBuilder.append(")");
        return stringBuilder.toString();
    }

    private String[] parsePatterns(String[] patterns, boolean removeParseMarks) {
        String[] returns = new String[patterns.length];

        for (int i = 0; i < patterns.length; i++) {
            if (removeParseMarks) {
                returns[i] = patterns[i].replaceAll("[^()\\[\\]|:\\s]*:", "");
            } else {
                returns[i] = patterns[i];
            }
        }
        return returns;
    }

    @SuppressWarnings("CallToPrintStackTrace")
    private void printToFile(JsonObject jsonElement) {
        File dataFolder = this.plugin.getDataFolder();
        if (!dataFolder.exists()) {
            if (!dataFolder.mkdirs()) {
                Utils.log("<red>Failed to create data folder");
                return;
            }
        }
        File file = new File(dataFolder, "json-docs.json");
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(jsonElement, writer);
            Utils.log("<green>Successfully wrote JSON element to 'json-docs.json'");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
