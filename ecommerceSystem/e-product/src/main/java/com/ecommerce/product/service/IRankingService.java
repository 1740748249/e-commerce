package com.ecommerce.product.service;

import com.ecommerce.product.domain.vo.RankingItemVO;

import java.util.List;

public interface IRankingService {

    List<RankingItemVO> getRanking(int limit);
}
