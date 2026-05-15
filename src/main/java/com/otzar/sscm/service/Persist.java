package com.otzar.sscm.service;

import com.otzar.sscm.entities.User;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional
@Component
@SuppressWarnings("unchecked")
public class Persist {

    private final SessionFactory sessionFactory;

    public Persist(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public <T> void saveAll(List<T> objects) {
        for (T object : objects) {
            sessionFactory.getCurrentSession().saveOrUpdate(object);
        }
    }

    public void remove(Object object) {
        sessionFactory.getCurrentSession().remove(object);
    }

    public Session getQuerySession() {
        return sessionFactory.getCurrentSession();
    }

    public void save(Object object) {
        sessionFactory.getCurrentSession().saveOrUpdate(object);
    }

    public <T> T loadObject(Class<T> clazz, long oid) {
        return getQuerySession().get(clazz, oid);
    }

    public <T> List<T> loadList(Class<T> clazz) {
        return sessionFactory.getCurrentSession()
                .createQuery("FROM " + clazz.getSimpleName()).list();
    }

    public <T> List<T> loadListByParameter(String hql, String parameterName, Object parameterValue, Class<T> clazz) {
        return sessionFactory.getCurrentSession()
                .createQuery(hql, clazz)
                .setParameter(parameterName, parameterValue)
                .list();
    }

    public User login (String username){
        return (User) sessionFactory.getCurrentSession().createQuery("FROM User WHERE username = :username")
                .setString("username", username)
                .uniqueResult();
    }
}
