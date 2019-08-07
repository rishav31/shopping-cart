package com.cart.controller;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.cart.dao.OrderDAO;
import com.cart.dao.ProductDAO;
import com.cart.entity.Order;
import com.cart.entity.Product;
import com.cart.model.CartInfo;
import com.cart.model.CustomerInfo;
import com.cart.model.OrderInfo;
import com.cart.model.PaginationResult;
import com.cart.model.ProductInfo;
import com.cart.dao.Utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@RestController
// Enable Hibernate Transaction.
@Transactional
public class MainController {

	@Autowired
	private OrderDAO orderDAO;

	@Autowired
	private ProductDAO productDAO;

	@GetMapping("/productList")
	public ResponseEntity<List<Product>> list() {
		List<Product> products = productDAO.getList();
		return ResponseEntity.ok().body(products);
	}

	@GetMapping("/buyProduct/{code}")
	public ResponseEntity<CartInfo> get(HttpServletRequest request, @PathVariable("code") String code) {
		Product product = null;
		CartInfo cartInfo = null;
		if (code != null && code.length() > 0) {
			product = productDAO.findProduct(code);
		}
		if (product != null) {

			// Cart info stored in Session.
			cartInfo = Utils.getCartInSession(request);

			ProductInfo productInfo = new ProductInfo(product);

			cartInfo.addProduct(productInfo, 1);
		}
		;
		return ResponseEntity.ok().body(cartInfo);
	}

	@RequestMapping({ "/shoppingCartRemoveProduct" })
	public String removeProductHandler(HttpServletRequest request, Model model, //
			@RequestParam(value = "code", defaultValue = "") String code) {
		Product product = null;
		if (code != null && code.length() > 0) {
			product = productDAO.findProduct(code);
		}
		if (product != null) {

			// Cart Info stored in Session.
			CartInfo cartInfo = Utils.getCartInSession(request);

			ProductInfo productInfo = new ProductInfo(product);

			cartInfo.removeProduct(productInfo);

		}
		// Redirect to shoppingCart page.
		return "redirect:/shoppingCart";
	}

	@DeleteMapping("/shoppingCartRemoveProduct/{id}")
	public ResponseEntity<?> delete(HttpServletRequest request, @PathVariable("id") String code) {
		Product product = null;
		OrderInfo order = null;
		if (code != null && code.length() > 0) {
			order = orderDAO.getOrderInfo(code);
		}
		if (order != null) {

			orderDAO.removeOrder(order.getId());
			return ResponseEntity.ok().body("Product has been deleted successfully.");
		} else {
			return ResponseEntity.ok().body("Failed");
		}

	}

	// POST: Update quantity of products in cart.
	@RequestMapping(value = { "/shoppingCart" }, method = RequestMethod.POST)
	public String shoppingCartUpdateQty(HttpServletRequest request, //
			Model model, //
			@ModelAttribute("cartForm") CartInfo cartForm) {

		CartInfo cartInfo = Utils.getCartInSession(request);
		cartInfo.updateQuantity(cartForm);

		// Redirect to shoppingCart page.
		return "redirect:/shoppingCart";
	}

	// GET: Show Cart
	@RequestMapping(value = { "/shoppingCart" }, method = RequestMethod.GET)
	public String shoppingCartHandler(HttpServletRequest request, Model model) {
		CartInfo myCart = Utils.getCartInSession(request);

		model.addAttribute("cartForm", myCart);
		return "shoppingCart";
	}

	// GET: Enter customer information.
	@RequestMapping(value = { "/shoppingCartCustomer" }, method = RequestMethod.GET)
	public String shoppingCartCustomerForm(HttpServletRequest request, Model model) {

		CartInfo cartInfo = Utils.getCartInSession(request);

		// Cart is empty.
		if (cartInfo.isEmpty()) {

			// Redirect to shoppingCart page.
			return "redirect:/shoppingCart";
		}

		CustomerInfo customerInfo = cartInfo.getCustomerInfo();
		if (customerInfo == null) {
			customerInfo = new CustomerInfo();
		}

		model.addAttribute("customerForm", customerInfo);

		return "shoppingCartCustomer";
	}

	// POST: Save customer information.
	@RequestMapping(value = { "/shoppingCartCustomer" }, method = RequestMethod.POST)
	public String shoppingCartCustomerSave(HttpServletRequest request, //
			Model model, //
			@ModelAttribute("customerForm") @Validated CustomerInfo customerForm, //
			BindingResult result, //
			final RedirectAttributes redirectAttributes) {

		// If has Errors.
		if (result.hasErrors()) {
			customerForm.setValid(false);
			// Forward to reenter customer info.
			return "shoppingCartCustomer";
		}

		customerForm.setValid(true);
		CartInfo cartInfo = Utils.getCartInSession(request);

		cartInfo.setCustomerInfo(customerForm);

		// Redirect to Confirmation page.
		return "redirect:/shoppingCartConfirmation";
	}

	// GET: Review Cart to confirm.
	@RequestMapping(value = { "/shoppingCartConfirmation" }, method = RequestMethod.GET)
	public String shoppingCartConfirmationReview(HttpServletRequest request, Model model) {
		CartInfo cartInfo = Utils.getCartInSession(request);

		// Cart have no products.
		if (cartInfo.isEmpty()) {
			// Redirect to shoppingCart page.
			return "redirect:/shoppingCart";
		} else if (!cartInfo.isValidCustomer()) {
			// Enter customer info.
			return "redirect:/shoppingCartCustomer";
		}

		return "shoppingCartConfirmation";
	}


	@PostMapping("/saveCart")
	public ResponseEntity<?> save(@RequestBody CartInfo cartInfo) {
		CartInfo cartInfoData = cartInfo;

		// Cart have no products.
		if (cartInfoData.isEmpty()) {
			// Redirect to shoppingCart page.
			return ResponseEntity.ok("Cart is empty");
		} else if (!cartInfoData.isValidCustomer()) {
			// Enter customer info.
			return ResponseEntity.ok("Valid Customer: "+cartInfoData.getCustomerInfo());
		}
		try {
			orderDAO.saveOrder(cartInfoData);
		} catch (Exception e) {
			// Need: Propagation.NEVER?
			return ResponseEntity.ok(e.getMessage());
		}
		
		return ResponseEntity.ok().body("New Order has been saved with ID:" + cartInfoData.getOrderNum());
	}

	@RequestMapping(value = { "/shoppingCartFinalize" }, method = RequestMethod.GET)
	public String shoppingCartFinalize(HttpServletRequest request, Model model) {

		CartInfo lastOrderedCart = Utils.getLastOrderedCartInSession(request);

		if (lastOrderedCart == null) {
			return "redirect:/shoppingCart";
		}

		return "shoppingCartFinalize";
	}

	@RequestMapping(value = { "/productImage" }, method = RequestMethod.GET)
	public void productImage(HttpServletRequest request, HttpServletResponse response, Model model,
			@RequestParam("code") String code) throws IOException {
		Product product = null;
		if (code != null) {
			product = this.productDAO.findProduct(code);
		}
		if (product != null && product.getImage() != null) {
			response.setContentType("image/jpeg, image/jpg, image/png, image/gif");
			response.getOutputStream().write(product.getImage());
		}
		response.getOutputStream().close();
	}

}
