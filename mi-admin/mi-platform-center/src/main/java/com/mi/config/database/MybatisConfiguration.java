package com.mi.config.database;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.*;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 * 配置mybatis
 * @author yesh
 *         (M.M)!
 *         Created by 2017/6/16.
 */
@Slf4j
@Configuration
@ConditionalOnClass({EnableTransactionManagement.class})
@MapperScan(basePackages={"com.mi.mapper"})
@Import({ DataBaseConfiguration.class})
public class MybatisConfiguration {

    @Value("${spring.datasource.type}")
    private Class<? extends DataSource> dataSourceType;

    @Value("${datasource.readSize}")
    private String dataSourceSize;

    @Resource(name = "masterDataSource")
    private DataSource dataSource;

    @Resource(name = "readDataSources")
    private List<DataSource> readDataSources;

    @Bean
    @ConditionalOnMissingBean
    public SqlSessionFactory sqlSessionFactory() throws Exception {
        SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
        sqlSessionFactoryBean.setDataSource(roundRobinDataSouceProxy());
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        sqlSessionFactoryBean.setMapperLocations(resolver.getResources("classpath:mybatis/*.xml"));
        sqlSessionFactoryBean.setTypeAliasesPackage("com.mi.model");
        sqlSessionFactoryBean.getObject().getConfiguration().setMapUnderscoreToCamelCase(true);
        return sqlSessionFactoryBean.getObject();
    }
    /**
     * 有多少个数据源就要配置多少个bean
     * @return
     */
    @Bean
    public AbstractRoutingDataSource roundRobinDataSouceProxy() {

        int size = Integer.parseInt(dataSourceSize);

        MyAbstractRoutingDataSource proxy = new MyAbstractRoutingDataSource(size);

        Map<Object, Object> targetDataSources = new HashMap<Object, Object>();

        // DataSource writeDataSource = SpringContextHolder.getBean("writeDataSource");
        //设置写库
        targetDataSources.put(DataSourceType.write.getType(),dataSource);

        // targetDataSources.put(DataSourceType.read.getType(),readDataSource);
        //设置多读库
        for (int i = 0; i < size; i++) {
            targetDataSources.put(i, readDataSources.get(i));
        }

        proxy.setDefaultTargetDataSource(dataSource);
        proxy.setTargetDataSources(targetDataSources);
        return proxy;
    }
}
