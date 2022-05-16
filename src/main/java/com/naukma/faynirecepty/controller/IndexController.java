package com.naukma.faynirecepty.controller;

import com.naukma.faynirecepty.config.CustomPasswordEncoder;
import com.naukma.faynirecepty.model.RecipeDto;
import com.naukma.faynirecepty.model.entity.Recipe;
import com.naukma.faynirecepty.model.entity.User;
import com.naukma.faynirecepty.service.RecipeService;
import com.naukma.faynirecepty.service.UserService;
import com.naukma.faynirecepty.service.ValidationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.awt.print.Book;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Controller
public class IndexController {

    @Autowired
    private RecipeService recipeService;

    @Autowired
    private UserService userService;

    @Autowired
    private CustomPasswordEncoder customPasswordEncoder;

    @RequestMapping({"/register", ""})
    public String register() {
        return "registration";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @RequestMapping(value = "/registerForm", method = RequestMethod.POST)
    public String form(@RequestParam(name = "username") String username,
                       @RequestParam(name = "psw") String psw,
                       Model model, HttpServletRequest request) {

        System.out.println(username + " " + psw);

        if (!ValidationService.isRegFormValid(username, psw)) {
            model.addAttribute("problem", "Username should only contain latin and numbers, " +
                    "password must be 8-20 characters long");

            System.out.println("hi, im a little bitch");

            return "forward:/register";
        }

        if (userService.getUserByUsername(username) != null) {
            model.addAttribute("problem", "This user already exists");
            return "forward:/register";
        }

        userService.createUser(username, customPasswordEncoder.encode(psw));

        return "redirect:/login";
    }

    @RequestMapping({"/", ""})
    public String index(Model model){

        List<Recipe> popular = recipeService.findPopular();

        model.addAttribute("popular", popular);

        return "index";
    }

    @RequestMapping({"/recipe/{id}", ""})
    public String showInfoPage(Model model, @PathVariable(name = "id") Long id, HttpSession session) {
        Recipe recipe = recipeService.findById(id);
        User creator = userService.getUserById(recipe.getCreatorId());

        model.addAttribute("recipe", recipe);
        model.addAttribute("creator", creator);
        if (recipe != null) {
            String liked = "no";

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (!(authentication instanceof AnonymousAuthenticationToken)) {
                String currentUserName = authentication.getName();

                Set<Recipe> likedRecipes = userService.getUserByUsername(currentUserName).getLikedRecipes();


//                if(likedRecipes != null) {
//                    liked = likedRecipes.contains(recipe)? "yes" : "no";
//                }
            }

//            RecipeDto recipeDto = new RecipeDto(recipe, liked);
            session.setAttribute("liked", liked);
            return "recipe";
        }
        return "wrong_id_recipe";
    }

    @PostMapping("/recipe/{id}/add-to-liked")
    public String addToWishList(Authentication authentication, HttpSession session, @PathVariable(name = "id") Long recipeId) {
        if (authentication == null)
            return "redirect:/";

        User byLogin = userService.getUserByUsername(authentication.getName());
        Set<Recipe> likedRecipes = byLogin.getLikedRecipes();

        if(likedRecipes == null) likedRecipes = new HashSet<>();

        likedRecipes.add(recipeService.findById(recipeId));

        userService.save(byLogin);

        return "redirect:/profile";
    }

    @PostMapping("/recipe/{id}/delete-from-liked")
    public String deleteFromWishList(Authentication authentication, HttpSession session, @PathVariable(name = "id") Long recipeId) {
        if (authentication == null)
            return "redirect:/";

        User byLogin = userService.getUserByUsername(authentication.getName());
        Set<Recipe> likedRecipes = byLogin.getLikedRecipes();

        if(likedRecipes == null) likedRecipes = new HashSet<>();

        likedRecipes.remove(recipeService.findById(recipeId));

        userService.save(byLogin);

        return "redirect:/profile";
    }

    @RequestMapping(value = "/find-recipes", method = RequestMethod.POST)
    public String form(@RequestParam(name = "searchInput") String search,
                       Model model, HttpSession session) {

        List<Recipe> searchRes = recipeService.findAllContaining(search);
        model.addAttribute("searchRes", searchRes);
        session.setAttribute("search", search);

        return "redirect:/search-result";
    }

    @RequestMapping(value = "/search-result", method = RequestMethod.GET)
    public String findGet(Model model, HttpSession session) {

        String search = (String) session.getAttribute("search");

        List<Recipe> searchRes = recipeService.findAllContaining(search);
        model.addAttribute("searchRes", searchRes);

        return "search-result";
    }

}
