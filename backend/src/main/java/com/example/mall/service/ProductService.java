package com.example.mall.service;

import com.example.mall.common.BizException;
import com.example.mall.dao.ProductDao;
import com.example.mall.dto.ProductRequest;
import com.example.mall.model.Product;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ProductService {

    private final ProductDao productDao;

    public ProductService(ProductDao productDao) {
        this.productDao = productDao;
    }

    /** 前台只看上架商品 */
    public List<Product> listOnSale(String keyword, String category, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 1);
        return productDao.search(keyword, category, 1, (safePage - 1) * safeSize, safeSize);
    }

    public long countOnSale(String keyword, String category) {
        return productDao.count(keyword, category, 1);
    }

    public List<Product> listAll(String keyword, String category, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 1);
        return productDao.search(keyword, category, null, (safePage - 1) * safeSize, safeSize);
    }

    public long countAll(String keyword, String category) {
        return productDao.count(keyword, category, null);
    }

    public Product getOnSale(long id) {
        Product p = productDao.findById(id);
        if (p == null || p.status() == null || p.status() != 1) {
            throw new BizException(404, "商品不存在或已下架");
        }
        return p;
    }

    public Product getAny(long id) {
        Product p = productDao.findById(id);
        if (p == null) {
            throw new BizException(404, "商品不存在");
        }
        return p;
    }

    @Transactional(rollbackFor = Exception.class)
    public Product create(ProductRequest req) {
        Product valid = validate(req, -1);
        Long id = productDao.insert(valid);
        return getAny(id == null ? 0 : id);
    }

    @Transactional(rollbackFor = Exception.class)
    public Product update(long id, ProductRequest req) {
        Product exist = getAny(id);
        Product valid = validate(req, id);
        productDao.update(id, valid);
        return getAny(exist.id());
    }

    @Transactional(rollbackFor = Exception.class)
    public void offline(long id) {
        getAny(id);
        productDao.offline(id);
    }

    private Product validate(ProductRequest req, long id) {
        if (req == null) {
            throw new BizException(400, "请求体不能为空");
        }
        String name = req.name() == null ? "" : req.name().trim();
        String category = req.category() == null ? "" : req.category().trim();
        String description = req.description() == null ? "" : req.description().trim();

        if (name.isEmpty() || name.length() > 128) {
            throw new BizException(400, "商品名称需为 1-128 个字符");
        }
        if (category.isEmpty() || category.length() > 32) {
            throw new BizException(400, "商品分类需为 1-32 个字符");
        }
        if (req.price() == null || req.price().compareTo(BigDecimal.ZERO) <= 0
                || req.price().compareTo(new BigDecimal("999999")) > 0) {
            throw new BizException(400, "价格必须在 0 到 999999 之间");
        }
        if (req.stock() == null || req.stock() < 0 || req.stock() > 1_000_000) {
            throw new BizException(400, "库存必须为 0 到 1000000 之间的整数");
        }
        if (description.length() > 1000) {
            throw new BizException(400, "商品描述不能超过 1000 字");
        }

        return new Product(id, name, category, req.price(), req.stock(),
                req.coverUrl(), description, 1, 0, null, null);
    }
}
