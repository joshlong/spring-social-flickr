/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.social.flickr.user;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class SecurityContext {

    private static Map<String, User> mapOfSessionsToUsers = new ConcurrentHashMap<String, User>();

    public static User getCurrentUser(HttpServletRequest r) {
        String sessionId = r.getSession(true).getId();
        return mapOfSessionsToUsers.get(sessionId);
    }

    public static void setCurrentUser(HttpServletRequest r, User user) {
        mapOfSessionsToUsers.put(session(r).getId(), user);
    }

    private static HttpSession session(HttpServletRequest request) {
        return request.getSession(true);
    }

    public static boolean userSignedIn(HttpServletRequest request) {
        return getCurrentUser(request) != null;
    }

    public static void remove(HttpServletRequest request) {
        mapOfSessionsToUsers.remove(session(request).getId());
    }

}
