package com.tp.facturacion.service;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tp.facturacion.dto.FacturaReporteDto;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

@Service
public class FacturaService {

    @PersistenceContext
    private EntityManager em;

    @Transactional(readOnly = true)
    public List<FacturaReporteDto> buscarFacturasFiltradas(Date fechaDesde, Date fechaHasta,
                                                           String estado, Double montoMinimo) {

        StringBuilder jpql = new StringBuilder(
            "SELECT new com.tp.facturacion.dto.FacturaReporteDto(" +
            "    f.numero, f.fechaEmision, COALESCE(c.denominacion, 'Consumidor Final'), " +
            "    ci.denominacion, pv.descripcion, f.importeTotal, COUNT(d)" +
            ") " +
            "FROM FacturaVenta f " +
            "LEFT JOIN f.cliente c " +
            "JOIN f.condicionIva ci " +
            "JOIN f.puntoVenta pv " +
            "LEFT JOIN f.detalles d " +
            "WHERE 1=1 ");

        Map<String, Object> params = new HashMap<>();

        if (fechaDesde != null) {
            jpql.append("AND f.fechaEmision >= :fechaDesde ");
            params.put("fechaDesde", fechaDesde);
        }
        if (fechaHasta != null) {
            // fechaEmision guarda fecha Y hora: para incluir todo el día "hasta",
            // comparamos con "menor que el día siguiente a las 00:00".
            jpql.append("AND f.fechaEmision < :fechaHastaExclusiva ");
            params.put("fechaHastaExclusiva", diaSiguiente(fechaHasta));
        }
        if (estado != null && !estado.trim().isEmpty()) {
            jpql.append("AND f.estado = :estado ");
            params.put("estado", estado.trim());
        }
        if (montoMinimo != null) {
            jpql.append("AND f.importeTotal >= :montoMinimo ");
            params.put("montoMinimo", montoMinimo);
        }

        jpql.append("GROUP BY f.id, f.numero, f.fechaEmision, c.denominacion, " +
                    "ci.denominacion, pv.descripcion, f.importeTotal " +
                    "ORDER BY f.fechaEmision, f.numero");

        TypedQuery<FacturaReporteDto> query =
                em.createQuery(jpql.toString(), FacturaReporteDto.class);
        params.forEach(query::setParameter);

        return query.getResultList();
    }

    private Date diaSiguiente(Date fecha) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(fecha);
        cal.add(Calendar.DAY_OF_MONTH, 1);
        return cal.getTime();
    }
}