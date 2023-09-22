package org.gridsuite.explore.server;

import lombok.NonNull;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.regex.Pattern;

import static org.gridsuite.explore.server.ExploreTest.*;
import static org.gridsuite.explore.server.services.MockRemoteServices.*;

/**
 * Extension to intercept exceptions to modify the message.
 */
@SuppressWarnings("RedundantThrows")
public class ExploreTestExtension implements AfterTestExecutionCallback, AfterEachCallback {
    @Override
    public void afterTestExecution(ExtensionContext context) throws Exception {
        context.getExecutionException().ifPresent(this::modifyMessage);
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        context.getExecutionException().ifPresent(this::modifyMessage);
    }

    @SuppressWarnings("removal")
    private void modifyMessage(@NonNull final Throwable ex) {
        try {
            final Field detailMessage = ReflectionUtils.findField(ex.getClass(), "detailMessage", String.class);
            if (System.getSecurityManager() == null) {
                ReflectionUtils.makeAccessible(detailMessage);
            } else {
                AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
                    ReflectionUtils.makeAccessible(detailMessage);
                    return null;
                });
            }
            ReflectionUtils.setField(detailMessage, ex, ex.getMessage()
                    .replaceAll(Pattern.quote(TEST_FILE), "\\${TEST_FILE}")
                    .replaceAll(Pattern.quote(TEST_FILE_WITH_ERRORS), "\\${TEST_FILE_WITH_ERRORS}")
                    .replaceAll(Pattern.quote(TEST_INCORRECT_FILE), "\\${TEST_INCORRECT_FILE}")
                    .replaceAll(Pattern.quote(CASE_UUID.toString()), "\\${CASE_UUID}")
                    .replaceAll(Pattern.quote(NON_EXISTING_CASE_UUID.toString()), "\\${NON_EXISTING_CASE_UUID}")
                    .replaceAll(Pattern.quote(PARENT_DIRECTORY_UUID.toString()), "\\${PARENT_DIRECTORY_UUID}")
                    .replaceAll(Pattern.quote(PARENT_DIRECTORY_WITH_ERROR_UUID.toString()), "\\${PARENT_DIRECTORY_WITH_ERROR_UUID}")
                    .replaceAll(Pattern.quote(PRIVATE_STUDY_UUID.toString()), "\\${PRIVATE_STUDY_UUID}")
                    .replaceAll(Pattern.quote(PUBLIC_STUDY_UUID.toString()), "\\${PUBLIC_STUDY_UUID}")
                    .replaceAll(Pattern.quote(FILTER_UUID.toString()), "\\${FILTER_UUID}")
                    .replaceAll(Pattern.quote(FILTER_UUID_2.toString()), "\\${FILTER_UUID_2}")
                    .replaceAll(Pattern.quote(SCRIPT_ID_BASE_FORM_CONTINGENCY_LIST_UUID.toString()), "\\${SCRIPT_ID_BASE_FORM_CONTINGENCY_LIST_UUID}")
                    .replaceAll(Pattern.quote(CONTINGENCY_LIST_UUID.toString()), "\\${CONTINGENCY_LIST_UUID}")
                    .replaceAll(Pattern.quote(INVALID_ELEMENT_UUID.toString()), "\\${INVALID_ELEMENT_UUID}")
                    .replaceAll(Pattern.quote(PARAMETERS_UUID.toString()), "\\${PARAMETERS_UUID}")
                    .replaceAll(Pattern.quote(STUDY_ERROR_NAME), "\\${STUDY_ERROR_NAME}")
                    .replaceAll(Pattern.quote(STUDY1), "\\${STUDY1}")
                    .replaceAll(Pattern.quote(CASE1), "\\${CASE1}")
                    .replaceAll(Pattern.quote(USER1), "\\${USER1}")
                    //.replaceAll(Pattern.quote(FILTER), "\\${FILTER}")
                    .replaceAll(Pattern.quote(FILTER1), "\\${FILTER1}")
                    .replaceAll(Pattern.quote(FILTER_CONTINGENCY_LIST), "\\${FILTER_CONTINGENCY_LIST}")
                    .replaceAll(Pattern.quote(FILTER_CONTINGENCY_LIST_2), "\\${FILTER_CONTINGENCY_LIST_2}"));
        } catch (Throwable err) {
            err.printStackTrace();
        }
        if (ArrayUtils.isNotEmpty(ex.getSuppressed())) {
            for (final Throwable suppressed : ex.getSuppressed()) {
                modifyMessage(suppressed);
            }
        }
    }
}
