package com.omarsaidi.loginguard.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Deep links of the single-page app (a reload on /events/12, a bookmarked page) are answered with
 * index.html, then the React router shows the page. The list is explicit: any other unknown path
 * stays a 404 instead of silently returning the app.
 */
@Controller
class SpaController {

	@GetMapping({ "/login", "/events", "/events/{id:\\d+}", "/users", "/model", "/operators" })
	String index() {
		return "forward:/index.html";
	}

}
