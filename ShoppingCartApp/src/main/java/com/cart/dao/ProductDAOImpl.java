package com.cart.dao;

import java.util.Date;
import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import com.cart.dao.ProductDAO;
import com.cart.entity.Product;
import com.cart.model.PaginationResult;
import com.cart.model.ProductInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
 
// Transactional for Hibernate
@Repository
@Transactional
public class ProductDAOImpl implements ProductDAO {
 
    @Autowired
    private SessionFactory sessionFactory;
 
    
    public Product findProduct(String code) {
        Session session = sessionFactory.getCurrentSession();
        Criteria crit = session.createCriteria(Product.class);
        crit.add(Restrictions.eq("code", code));
        return (Product) crit.uniqueResult();
    }
 
    
    public ProductInfo findProductInfo(String code) {
        Product product = this.findProduct(code);
        if (product == null) {
            return null;
        }
        return new ProductInfo(product.getCode(), product.getName(), product.getPrice());
    }
 
    
    public void save(ProductInfo productInfo) {
        String code = productInfo.getCode();
 
        Product product = null;
 
        boolean isNew = false;
        if (code != null) {
            product = this.findProduct(code);
        }
        if (product == null) {
            isNew = true;
            product = new Product();
            product.setCreateDate(new Date());
        }
        product.setCode(code);
        product.setName(productInfo.getName());
        product.setPrice(productInfo.getPrice());
 
        if (productInfo.getFileData() != null) {
            byte[] image = productInfo.getFileData().getBytes();
            if (image != null && image.length > 0) {
                product.setImage(image);
            }
        }
        if (isNew) {
            this.sessionFactory.getCurrentSession().persist(product);
        }
        // If error in DB, Exceptions will be thrown out immediately
        this.sessionFactory.getCurrentSession().flush();
    }
 
    
    public PaginationResult<ProductInfo> queryProducts(int page, int maxResult, int maxNavigationPage,
            String likeName) {
        String sql = "Select new " + ProductInfo.class.getName() //
                + "(p.code, p.name, p.price) " + " from "//
                + Product.class.getName() + " p ";
        if (likeName != null && likeName.length() > 0) {
            sql += " Where lower(p.name) like :likeName ";
        }
        sql += " order by p.createDate desc ";
        //
        Session session = sessionFactory.getCurrentSession();
 
        Query query = session.createQuery(sql);
        if (likeName != null && likeName.length() > 0) {
            query.setParameter("likeName", "%" + likeName.toLowerCase() + "%");
        }
        return new PaginationResult<ProductInfo>(query, page, maxResult, maxNavigationPage);
    }
 
    
    public PaginationResult<ProductInfo> queryProducts(int page, int maxResult, int maxNavigationPage) {
        return queryProducts(page, maxResult, maxNavigationPage, null);
    }


	public List<Product> getList() {
		Session session = sessionFactory.getCurrentSession();
	      CriteriaBuilder cb = session.getCriteriaBuilder();
	      CriteriaQuery<Product> cq = cb.createQuery(Product.class);
	      Root<Product> root = cq.from(Product.class);
	      cq.select(root);
	      Query<Product> query = session.createQuery(cq);
	      return query.getResultList();
	}
    
}
