/*
 * Copyright (c) 2012, Francis Galiegue <fgaliegue@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Lesser GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Lesser GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.eel.kitchen.jsonschema.keyword;

import com.fasterxml.jackson.databind.JsonNode;
import org.eel.kitchen.jsonschema.main.JsonSchemaException;
import org.eel.kitchen.jsonschema.main.JsonSchemaFactory;
import org.eel.kitchen.jsonschema.ref.SchemaContainer;
import org.eel.kitchen.jsonschema.main.ValidationContext;
import org.eel.kitchen.jsonschema.report.ValidationReport;
import org.eel.kitchen.jsonschema.util.JsonLoader;
import org.eel.kitchen.jsonschema.validator.JsonValidatorCache;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static org.testng.Assert.*;

public abstract class AbstractKeywordValidatorTest
{
    private static final JsonSchemaFactory factory
        = new JsonSchemaFactory.Builder().build();
    private static final JsonValidatorCache cache = factory.getValidatorCache();

    private final JsonNode testData;
    private final Constructor<? extends KeywordValidator> constructor;

    AbstractKeywordValidatorTest(final Class<? extends KeywordValidator> c,
        final String resourceName)
        throws IOException, NoSuchMethodException
    {
        final String input = "/keyword/" + resourceName + ".json";
        testData = JsonLoader.fromResource(input);

        constructor = c.getConstructor(JsonNode.class);
    }

    @DataProvider
    protected Iterator<Object[]> getData()
    {
        final Set<Object[]> set = new HashSet<Object[]>(testData.size());

        for (final JsonNode node: testData)
            set.add(mungeArguments(node));

        return set.iterator();
    }

    private static Object[] mungeArguments(final JsonNode node)
    {
        return new Object[] {
            node.get("schema"),
            node.get("data"),
            node.get("valid").booleanValue(),
            node.get("messages")
        };
    }

    @Test(dataProvider = "getData", invocationCount = 10, threadPoolSize = 4)
    public final void testKeyword(final JsonNode schema, final JsonNode data,
        final boolean valid, final JsonNode messages)
        throws InvocationTargetException, IllegalAccessException,
        InstantiationException, JsonSchemaException
    {
        final KeywordValidator validator = constructor.newInstance(schema);
        final ValidationReport report = new ValidationReport();

        final ValidationContext context = new ValidationContext(cache);
        context.setContainer(new SchemaContainer(schema));
        validator.validate(context, report, data);

        assertEquals(report.isSuccess(), valid);

        if (valid)
            return;

        final JsonNode[] actual = toArray(report.asJsonNode().get(""));
        final JsonNode[] expected = toArray(messages);

        assertEqualsNoOrder(actual, expected);
    }

    private static JsonNode[] toArray(final JsonNode node)
    {
        final int size = node.size();
        final JsonNode[] ret = new JsonNode[size];

        int idx = 0;

        for (final JsonNode element: node) {
            ret[idx] = element;
            idx++;
        }

        return ret;
    }
}
