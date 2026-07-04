package com.ecommerce.product.controller;

import com.ecommerce.common.domain.R;
import com.ecommerce.product.domain.vo.RankingItemVO;
import com.ecommerce.product.service.IRankingService;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ranking")
@Api(tags = "销量排行榜相关接口")
@RequiredArgsConstructor
public class RankingController {
    private final IRankingService rankingService;

    @GetMapping
    public R<List<RankingItemVO>> list(@RequestParam(defaultValue = "10") int limit) {
        return R.ok(rankingService.getRanking(limit));
    }
}
